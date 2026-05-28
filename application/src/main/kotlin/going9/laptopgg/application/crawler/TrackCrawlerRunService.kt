package going9.laptopgg.application.crawler

import going9.laptopgg.application.port.out.CrawlerRunPort
import going9.laptopgg.domain.crawler.CrawlerRun
import going9.laptopgg.domain.crawler.CrawlerRunStatus
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrackCrawlerRunService(
    private val crawlerRunPort: CrawlerRunPort,
) : TrackCrawlerRunUseCase {
    @Transactional
    override fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRun {
        return crawlerRunPort.save(
            CrawlerRun(
                filterProfile = filterProfile,
                startPage = startPage,
                limitCount = limit,
            ),
        )
    }

    @Transactional
    override fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRun {
        return crawlerRunPort.save(
            CrawlerRun(
                filterProfile = filterProfile,
                startPage = startPage,
                limitCount = limit,
                status = CrawlerRunStatus.SKIPPED_LOCKED,
                endedAt = LocalDateTime.now(),
                errorMessage = "Another crawler run already holds the PostgreSQL advisory lock.",
            ),
        )
    }

    @Transactional
    override fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunStatus,
        errorMessage: String?,
    ): CrawlerRun {
        require(status == CrawlerRunStatus.SUCCEEDED || status == CrawlerRunStatus.FAILED) {
            "Crawler run can only finish as SUCCEEDED or FAILED."
        }

        val crawlerRun = crawlerRunPort.findById(runId)
            ?: throw IllegalArgumentException("Crawler run not found: $runId")

        crawlerRun.status = status
        crawlerRun.processedCount = summary.processedCount
        crawlerRun.createdCount = summary.createdCount
        crawlerRun.updatedCount = summary.updatedCount
        crawlerRun.degradedCount = summary.degradedCount
        crawlerRun.failedCount = summary.failedCount
        crawlerRun.failureSamples = summary.failureSamples.toStorageText()
        crawlerRun.errorMessage = errorMessage?.truncateForStorage()
        crawlerRun.endedAt = LocalDateTime.now()

        return crawlerRun
    }

    @Transactional
    override fun fail(runId: Long, exception: Throwable): CrawlerRun {
        val crawlerRun = crawlerRunPort.findById(runId)
            ?: throw IllegalArgumentException("Crawler run not found: $runId")

        crawlerRun.status = CrawlerRunStatus.FAILED
        crawlerRun.errorMessage = (exception.message ?: exception::class.simpleName ?: "Unknown crawler failure")
            .truncateForStorage()
        crawlerRun.endedAt = LocalDateTime.now()

        return crawlerRun
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
