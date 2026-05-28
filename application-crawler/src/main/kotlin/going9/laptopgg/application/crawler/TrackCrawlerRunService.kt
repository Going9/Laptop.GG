package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawlerRunPort
import going9.laptopgg.domain.crawler.CrawlerRun
import going9.laptopgg.domain.crawler.CrawlerRunStatus
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@Transactional
class TrackCrawlerRunService(
    private val crawlerRunPort: CrawlerRunPort,
) : TrackCrawlerRunUseCase {
    override fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
        return crawlerRunPort.save(
            CrawlerRun(
                filterProfile = filterProfile,
                startPage = startPage,
                limitCount = limit,
            ),
        ).toRecord()
    }

    override fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
        return crawlerRunPort.save(
            CrawlerRun(
                filterProfile = filterProfile,
                startPage = startPage,
                limitCount = limit,
                status = CrawlerRunStatus.SKIPPED_LOCKED,
                endedAt = LocalDateTime.now(),
                errorMessage = "Another crawler run already holds the PostgreSQL advisory lock.",
            ),
        ).toRecord()
    }

    override fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String?,
    ): CrawlerRunRecord {
        val crawlerRun = crawlerRunPort.findById(runId)
            ?: throw IllegalArgumentException("Crawler run not found: $runId")

        crawlerRun.status = status.toDomainStatus()
        crawlerRun.processedCount = summary.processedCount
        crawlerRun.createdCount = summary.createdCount
        crawlerRun.updatedCount = summary.updatedCount
        crawlerRun.degradedCount = summary.degradedCount
        crawlerRun.failedCount = summary.failedCount
        crawlerRun.failureSamples = summary.failureSamples.toStorageText()
        crawlerRun.errorMessage = errorMessage?.truncateForStorage()
        crawlerRun.endedAt = LocalDateTime.now()

        return crawlerRun.toRecord()
    }

    override fun fail(runId: Long, exception: Throwable): CrawlerRunRecord {
        val crawlerRun = crawlerRunPort.findById(runId)
            ?: throw IllegalArgumentException("Crawler run not found: $runId")

        crawlerRun.status = CrawlerRunStatus.FAILED
        crawlerRun.errorMessage = (exception.message ?: exception::class.simpleName ?: "Unknown crawler failure")
            .truncateForStorage()
        crawlerRun.endedAt = LocalDateTime.now()

        return crawlerRun.toRecord()
    }

    private fun CrawlerRunCompletionStatus.toDomainStatus(): CrawlerRunStatus {
        return when (this) {
            CrawlerRunCompletionStatus.SUCCEEDED -> CrawlerRunStatus.SUCCEEDED
            CrawlerRunCompletionStatus.FAILED -> CrawlerRunStatus.FAILED
        }
    }

    private fun CrawlerRun.toRecord(): CrawlerRunRecord {
        return CrawlerRunRecord(
            id = id,
            status = when (status) {
                CrawlerRunStatus.RUNNING -> CrawlerRunStatusResult.RUNNING
                CrawlerRunStatus.SUCCEEDED -> CrawlerRunStatusResult.SUCCEEDED
                CrawlerRunStatus.FAILED -> CrawlerRunStatusResult.FAILED
                CrawlerRunStatus.SKIPPED_LOCKED -> CrawlerRunStatusResult.SKIPPED_LOCKED
            },
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
