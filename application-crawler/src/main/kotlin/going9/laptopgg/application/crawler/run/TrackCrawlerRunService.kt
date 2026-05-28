package going9.laptopgg.application.crawler.run

import going9.laptopgg.application.crawler.port.out.CrawlerRunPort
import going9.laptopgg.application.crawler.port.out.CrawlerTransactionPort
import java.time.LocalDateTime

class TrackCrawlerRunService(
    private val crawlerRunPort: CrawlerRunPort,
    private val transactionPort: CrawlerTransactionPort,
) : TrackCrawlerRunUseCase {
    override fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
        return transactionPort.write {
            crawlerRunPort.create(
                CreateCrawlerRunCommand(
                    filterProfile = filterProfile,
                    startPage = startPage,
                    limitCount = limit,
                ),
            ).toRecord()
        }
    }

    override fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
        return transactionPort.write {
            crawlerRunPort.create(
                CreateCrawlerRunCommand(
                    filterProfile = filterProfile,
                    startPage = startPage,
                    limitCount = limit,
                    status = CrawlerRunStatusResult.SKIPPED_LOCKED,
                    endedAt = LocalDateTime.now(),
                    errorMessage = "Another crawler run already holds the PostgreSQL advisory lock.",
                ),
            ).toRecord()
        }
    }

    override fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String?,
    ): CrawlerRunRecord {
        return transactionPort.write {
            (
                crawlerRunPort.update(
                    UpdateCrawlerRunCommand(
                        runId = runId,
                        status = status.toStatusResult(),
                        processedCount = summary.processedCount,
                        createdCount = summary.createdCount,
                        updatedCount = summary.updatedCount,
                        degradedCount = summary.degradedCount,
                        failedCount = summary.failedCount,
                        failureSamples = summary.failureSamples.toStorageText(),
                        errorMessage = errorMessage?.truncateForStorage(),
                        endedAt = LocalDateTime.now(),
                    ),
                ) ?: throw IllegalArgumentException("Crawler run not found: $runId")
            ).toRecord()
        }
    }

    override fun fail(runId: Long, exception: Throwable): CrawlerRunRecord {
        return transactionPort.write {
            (
                crawlerRunPort.update(
                    UpdateCrawlerRunCommand(
                        runId = runId,
                        status = CrawlerRunStatusResult.FAILED,
                        errorMessage = (exception.message ?: exception::class.simpleName ?: "Unknown crawler failure")
                            .truncateForStorage(),
                        endedAt = LocalDateTime.now(),
                    ),
                ) ?: throw IllegalArgumentException("Crawler run not found: $runId")
            ).toRecord()
        }
    }

    private fun CrawlerRunCompletionStatus.toStatusResult(): CrawlerRunStatusResult {
        return when (this) {
            CrawlerRunCompletionStatus.SUCCEEDED -> CrawlerRunStatusResult.SUCCEEDED
            CrawlerRunCompletionStatus.FAILED -> CrawlerRunStatusResult.FAILED
        }
    }

    private fun CrawlerRunState.toRecord(): CrawlerRunRecord {
        return CrawlerRunRecord(
            id = id,
            status = status,
        )
    }

    private fun List<String>.toStorageText(): String? {
        return take(MAX_STORED_SAMPLES)
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
            ?.truncateForStorage()
    }

    private fun String.truncateForStorage(): String {
        return take(MAX_TEXT_LENGTH)
    }

    private companion object {
        const val MAX_STORED_SAMPLES = 20
        const val MAX_TEXT_LENGTH = 4_000
    }
}
