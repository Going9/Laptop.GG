package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.run.CrawlerRunRecord
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.job.crawler.orchestration.CrawlSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlerJobSummaryLogger {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun logLockFailure(exception: Throwable) {
        logger.error("Crawler lock acquisition failed.", exception)
    }

    fun logSkipped(run: CrawlerRunRecord, request: CrawlerJobRequest) {
        logger.warn(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 degradedCount=0 failedCount=0",
            run.id,
            run.status,
            request.filterProfile,
            request.startPage,
            request.limitLabel(),
        )
    }

    fun logCompleted(
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
            request.limitLabel(),
            summary.processedCount,
            summary.createdCount,
            summary.updatedCount,
            summary.degradedCount,
            summary.failedCount,
        )

        if (summary.degradedSamples.isNotEmpty()) {
            logger.warn("Crawler degraded samples: {}", summary.degradedSamples)
        }
        if (summary.failureSamples.isNotEmpty()) {
            logger.warn("Crawler failure samples: {}", summary.failureSamples)
        }
    }

    fun logRunFailure(runId: Long, request: CrawlerJobRequest, exception: Throwable) {
        logger.error("Crawler run failed. runId={}", runId, exception)
        logger.error(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 degradedCount=0 failedCount=1",
            runId,
            CrawlerRunStatusResult.FAILED,
            request.filterProfile,
            request.startPage,
            request.limitLabel(),
        )
    }

    private fun CrawlerJobRequest.limitLabel(): String {
        return limit?.toString() ?: "ALL"
    }
}
