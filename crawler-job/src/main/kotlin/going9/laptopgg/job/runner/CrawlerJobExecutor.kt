package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.application.crawler.run.CrawlerRunSummary
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.job.crawler.orchestration.CrawlSummary
import going9.laptopgg.job.crawler.orchestration.CrawlerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlerJobExecutor(
    private val crawlerService: CrawlerService,
    private val crawlerRunLockUseCase: CrawlerRunLockUseCase,
    private val trackCrawlerRunUseCase: TrackCrawlerRunUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(request: CrawlerJobRequest): Int {
        val lockResult = runCatching {
            crawlerRunLockUseCase.runLocked {
                runTrackedCrawler(request)
            }
        }.getOrElse { exception ->
            logger.error("Crawler lock acquisition failed.", exception)
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
        logger.warn(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 degradedCount=0 failedCount=0",
            skippedRun.id,
            skippedRun.status,
            request.filterProfile,
            request.startPage,
            request.limit ?: "ALL",
        )
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
            logCrawlerSummary(runId, finishedStatus, request, summary)
            if (summary.degradedSamples.isNotEmpty()) {
                logger.warn("Crawler degraded samples: {}", summary.degradedSamples)
            }
            if (summary.failureSamples.isNotEmpty()) {
                logger.warn("Crawler failure samples: {}", summary.failureSamples)
            }
            if (summary.failedCount == 0) 0 else 1
        }.getOrElse { exception ->
            trackCrawlerRunUseCase.fail(runId, exception)
            logger.error("Crawler run failed. runId={}", runId, exception)
            logger.error(
                "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 degradedCount=0 failedCount=1",
                runId,
                CrawlerRunStatusResult.FAILED,
                request.filterProfile,
                request.startPage,
                request.limit ?: "ALL",
            )
            1
        }
    }

    private fun logCrawlerSummary(
        runId: Long,
        status: CrawlerRunCompletionStatus,
        request: CrawlerJobRequest,
        summary: CrawlSummary,
    ) {
        logger.info(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount={} createdCount={} updatedCount={} degradedCount={} failedCount={}",
            runId,
            status,
            request.filterProfile,
            request.startPage,
            request.limit ?: "ALL",
            summary.processedCount,
            summary.createdCount,
            summary.updatedCount,
            summary.degradedCount,
            summary.failedCount,
        )
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
