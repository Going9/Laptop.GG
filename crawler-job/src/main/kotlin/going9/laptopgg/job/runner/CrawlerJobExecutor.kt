package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.CrawlerRunSummary
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.job.crawler.orchestration.CrawlFailedWithPartialSummary
import going9.laptopgg.job.crawler.orchestration.CrawlSummary
import going9.laptopgg.job.crawler.orchestration.CrawlerService
import going9.laptopgg.job.crawler.support.isCrawlerInterruptedFailure
import org.springframework.stereotype.Component

@Component
internal class CrawlerJobExecutor(
    private val crawlerService: CrawlerService,
    private val crawlerRunLockUseCase: CrawlerRunLockUseCase,
    private val trackCrawlerRunUseCase: TrackCrawlerRunUseCase,
    private val crawlerJobSummaryLogger: CrawlerJobSummaryLogger,
) {
    fun execute(request: CrawlerJobRequest): Int {
        val lockResult = try {
            crawlerRunLockUseCase.runLocked {
                runTrackedCrawler(request)
            }
        } catch (exception: Exception) {
            if (exception.isCrawlerInterruptedFailure()) {
                throw exception
            }
            crawlerJobSummaryLogger.logLockFailure(exception)
            return 1
        }

        if (lockResult.acquired) {
            return lockResult.value ?: 1
        }

        val skippedRun = trackCrawlerRunUseCase.skipLocked(
            filterProfile = request.filterProfile,
            startPage = request.startPage,
            limit = request.limit,
        )
        crawlerJobSummaryLogger.logSkipped(skippedRun, request)
        return 0
    }

    private fun runTrackedCrawler(request: CrawlerJobRequest): Int {
        val crawlerRun = trackCrawlerRunUseCase.start(
            filterProfile = request.filterProfile,
            startPage = request.startPage,
            limit = request.limit,
        )
        val runId = crawlerRun.id

        return try {
            val summary = crawlerService.crawlAll(
                limit = request.limit,
                startPage = request.startPage,
                filterProfile = request.filterProfile,
            )
            val finishedStatus = if (summary.failedCount == 0) {
                CrawlerRunCompletionStatus.SUCCEEDED
            } else {
                CrawlerRunCompletionStatus.FAILED
            }
            val errorMessage = if (summary.failedCount == 0) {
                null
            } else {
                "Crawler finished with ${summary.failedCount} failed item(s)."
            }
            trackCrawlerRunUseCase.finish(
                runId = runId,
                summary = summary.toRunSummary(),
                status = finishedStatus,
                errorMessage = errorMessage,
            )
            crawlerJobSummaryLogger.logCompleted(runId, finishedStatus, request, summary)
            if (summary.failedCount == 0) 0 else 1
        } catch (failure: Throwable) {
            val interrupted = failure.isCrawlerInterruptedFailure()
            recordRunFailure(
                runId = runId,
                request = request,
                failure = failure,
                partialSummary = (failure as? CrawlFailedWithPartialSummary)?.partialSummary,
            )
            if (interrupted) {
                throw failure
            }
            if (failure is Exception) {
                1
            } else {
                throw failure
            }
        }
    }

    private fun recordRunFailure(
        runId: Long,
        request: CrawlerJobRequest,
        failure: Throwable,
        partialSummary: CrawlSummary?,
    ) {
        try {
            if (partialSummary == null) {
                trackCrawlerRunUseCase.fail(runId, failure)
            } else {
                trackCrawlerRunUseCase.finish(
                    runId = runId,
                    summary = partialSummary.toRunSummary(),
                    status = CrawlerRunCompletionStatus.FAILED,
                    errorMessage = failure.toCrawlerErrorMessage(),
                )
            }
        } catch (trackingFailure: Throwable) {
            failure.addSuppressed(trackingFailure)
        }
        crawlerJobSummaryLogger.logRunFailure(runId, request, failure, partialSummary)
    }

    private fun Throwable.toCrawlerErrorMessage(): String {
        return cause?.message ?: message ?: javaClass.simpleName ?: "Unknown crawler failure"
    }

    private fun CrawlSummary.toRunSummary(): CrawlerRunSummary {
        return CrawlerRunSummary(
            processedCount = processedCount,
            createdCount = createdCount,
            updatedCount = updatedCount,
            detailRefreshCount = detailRefreshCount,
            priceOnlyUpdatedCount = priceOnlyUpdatedCount,
            degradedCount = degradedCount,
            failedCount = failedCount,
            failureSamples = failureSamples,
        )
    }
}

internal data class CrawlerJobRequest(
    val limit: Int?,
    val startPage: Int,
    val filterProfile: CrawlerFilterProfile,
)
