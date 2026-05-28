package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.CrawlerRunSummary
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.job.crawler.orchestration.CrawlSummary
import going9.laptopgg.job.crawler.orchestration.CrawlerService
import org.springframework.stereotype.Component

@Component
class CrawlerJobExecutor(
    private val crawlerService: CrawlerService,
    private val crawlerRunLockUseCase: CrawlerRunLockUseCase,
    private val trackCrawlerRunUseCase: TrackCrawlerRunUseCase,
    private val crawlerJobSummaryLogger: CrawlerJobSummaryLogger,
) {
    fun execute(request: CrawlerJobRequest): Int {
        val lockResult = runCatching {
            crawlerRunLockUseCase.runLocked {
                runTrackedCrawler(request)
            }
        }.getOrElse { exception ->
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
        val runId = requireNotNull(crawlerRun.id)

        return runCatching {
            val summary = crawlerService.crawlAll(
                limit = request.limit,
                startPage = request.startPage,
                filterProfileRaw = request.filterProfile,
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
        }.getOrElse { exception ->
            trackCrawlerRunUseCase.fail(runId, exception)
            crawlerJobSummaryLogger.logRunFailure(runId, request, exception)
            1
        }
    }

    private fun CrawlSummary.toRunSummary(): CrawlerRunSummary {
        return CrawlerRunSummary(
            processedCount = processedCount,
            createdCount = createdCount,
            updatedCount = updatedCount,
            degradedCount = degradedCount,
            failedCount = failedCount,
            failureSamples = failureSamples,
        )
    }
}

data class CrawlerJobRequest(
    val limit: Int?,
    val startPage: Int,
    val filterProfile: String,
)
