package going9.laptopgg.application.crawler.run

import java.time.LocalDateTime

internal class CrawlerRunCommandFactory(
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    fun start(filterProfile: String, startPage: Int, limit: Int?): CreateCrawlerRunCommand {
        return CreateCrawlerRunCommand(
            filterProfile = filterProfile,
            startPage = startPage,
            limitCount = limit,
            startedAt = now(),
        )
    }

    fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CreateCrawlerRunCommand {
        val timestamp = now()
        return CreateCrawlerRunCommand(
            filterProfile = filterProfile,
            startPage = startPage,
            limitCount = limit,
            startedAt = timestamp,
            status = CrawlerRunStatusResult.SKIPPED_LOCKED,
            endedAt = timestamp,
            errorMessage = "Another crawler run already holds the PostgreSQL advisory lock.",
        )
    }

    fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String?,
    ): UpdateCrawlerRunCommand {
        return UpdateCrawlerRunCommand(
            runId = runId,
            status = status.toStatusResult(),
            processedCount = summary.processedCount,
            createdCount = summary.createdCount,
            updatedCount = summary.updatedCount,
            detailRefreshCount = summary.detailRefreshCount,
            priceOnlyUpdatedCount = summary.priceOnlyUpdatedCount,
            degradedCount = summary.degradedCount,
            failedCount = summary.failedCount,
            failureSamples = summary.failureSamples.toStorageText(),
            errorMessage = errorMessage?.truncateForStorage(),
            endedAt = now(),
        )
    }

    fun fail(runId: Long, failure: Throwable): UpdateCrawlerRunCommand {
        return UpdateCrawlerRunCommand(
            runId = runId,
            status = CrawlerRunStatusResult.FAILED,
            errorMessage = (failure.message ?: failure::class.simpleName ?: "Unknown crawler failure")
                .truncateForStorage(),
            endedAt = now(),
        )
    }

    private fun CrawlerRunCompletionStatus.toStatusResult(): CrawlerRunStatusResult {
        return when (this) {
            CrawlerRunCompletionStatus.SUCCEEDED -> CrawlerRunStatusResult.SUCCEEDED
            CrawlerRunCompletionStatus.FAILED -> CrawlerRunStatusResult.FAILED
        }
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
