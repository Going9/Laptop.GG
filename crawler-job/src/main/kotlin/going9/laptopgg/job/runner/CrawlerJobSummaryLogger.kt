package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.run.CrawlerRunRecord
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.job.crawler.orchestration.CrawlSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class CrawlerJobSummaryLogger {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun logLockFailure(exception: Exception) {
        logger.error("Crawler lock acquisition failed.", exception)
    }

    fun logSkipped(run: CrawlerRunRecord, request: CrawlerJobRequest) {
        logger.warn(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 detailRefreshCount=0 priceOnlyUpdatedCount=0 degradedCount=0 failedCount=0",
            run.id,
            run.status,
            request.filterProfile.storageValue,
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
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount={} createdCount={} updatedCount={} detailRefreshCount={} priceOnlyUpdatedCount={} degradedCount={} failedCount={}",
            runId,
            status,
            request.filterProfile.storageValue,
            request.startPage,
            request.limitLabel(),
            summary.processedCount,
            summary.createdCount,
            summary.updatedCount,
            summary.detailRefreshCount,
            summary.priceOnlyUpdatedCount,
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

    fun logRunFailure(
        runId: Long,
        request: CrawlerJobRequest,
        failure: Throwable,
        partialSummary: CrawlSummary? = null,
    ) {
        logger.error("Crawler run failed. runId={}", runId, failure)
        logger.error(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount={} createdCount={} updatedCount={} detailRefreshCount={} priceOnlyUpdatedCount={} degradedCount={} failedCount={}",
            runId,
            CrawlerRunStatusResult.FAILED,
            request.filterProfile.storageValue,
            request.startPage,
            request.limitLabel(),
            partialSummary?.processedCount ?: 0,
            partialSummary?.createdCount ?: 0,
            partialSummary?.updatedCount ?: 0,
            partialSummary?.detailRefreshCount ?: 0,
            partialSummary?.priceOnlyUpdatedCount ?: 0,
            partialSummary?.degradedCount ?: 0,
            partialSummary?.failedCount ?: 1,
        )
    }

    private fun CrawlerJobRequest.limitLabel(): String {
        return limit?.toString() ?: "ALL"
    }
}
