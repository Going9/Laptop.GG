package going9.laptopgg.application.crawler.run

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort

internal class TrackCrawlerRunService(
    private val crawlerRunPort: CrawlerRunPort,
    private val transactionPort: CrawlerTransactionPort,
    private val commandFactory: CrawlerRunCommandFactory = CrawlerRunCommandFactory(),
) : TrackCrawlerRunUseCase {
    override fun start(filterProfile: CrawlerFilterProfile, startPage: Int, limit: Int?): CrawlerRunRecord {
        validateStartRequest(startPage, limit)
        return transactionPort.write {
            crawlerRunPort.create(commandFactory.start(filterProfile, startPage, limit)).toRecord()
        }
    }

    override fun skipLocked(filterProfile: CrawlerFilterProfile, startPage: Int, limit: Int?): CrawlerRunRecord {
        validateStartRequest(startPage, limit)
        return transactionPort.write {
            crawlerRunPort.create(commandFactory.skipLocked(filterProfile, startPage, limit)).toRecord()
        }
    }

    override fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String?,
    ): CrawlerRunRecord {
        validateRunId(runId)
        validateSummary(summary)
        return transactionPort.write {
            (
                crawlerRunPort.update(commandFactory.finish(runId, summary, status, errorMessage))
                    ?: throw CrawlerResourceNotFoundException("CrawlerRun", runId)
            ).toRecord()
        }
    }

    override fun fail(runId: Long, failure: Throwable): CrawlerRunRecord {
        validateRunId(runId)
        return transactionPort.write {
            (
                crawlerRunPort.update(commandFactory.fail(runId, failure))
                    ?: throw CrawlerResourceNotFoundException("CrawlerRun", runId)
            ).toRecord()
        }
    }

    private fun validateStartRequest(startPage: Int, limit: Int?) {
        if (startPage <= 0) {
            throw CrawlerInvalidCommandException("startPage must be positive.")
        }
        if (limit != null && limit <= 0) {
            throw CrawlerInvalidCommandException("limit must be positive.")
        }
    }

    private fun validateRunId(runId: Long) {
        if (runId <= 0) {
            throw CrawlerInvalidCommandException("runId must be positive.")
        }
    }

    private fun validateSummary(summary: CrawlerRunSummary) {
        val counts = listOf(
            summary.processedCount,
            summary.createdCount,
            summary.updatedCount,
            summary.detailRefreshCount,
            summary.priceOnlyUpdatedCount,
            summary.degradedCount,
            summary.failedCount,
        )
        if (counts.any { it < 0 }) {
            throw CrawlerInvalidCommandException("crawler run summary counts must not be negative.")
        }
    }

    private fun CrawlerRunState.toRecord(): CrawlerRunRecord {
        return CrawlerRunRecord(
            id = id,
            status = status,
        )
    }
}
