package going9.laptopgg.application.crawler.run

import java.time.LocalDateTime

data class CrawlerLockResult<T>(
    val acquired: Boolean,
    val value: T?,
)

data class CreateCrawlerRunCommand(
    val filterProfile: CrawlerFilterProfile,
    val startPage: Int,
    val limitCount: Int?,
    val startedAt: LocalDateTime,
    val status: CrawlerRunStatusResult = CrawlerRunStatusResult.RUNNING,
    val endedAt: LocalDateTime? = null,
    val errorMessage: String? = null,
)

sealed interface UpdateCrawlerRunCommand {
    val runId: Long
    val status: CrawlerRunStatusResult
    val errorMessage: String?
    val endedAt: LocalDateTime
}

data class CompleteCrawlerRunCommand(
    override val runId: Long,
    override val status: CrawlerRunStatusResult,
    val processedCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val detailRefreshCount: Int,
    val priceOnlyUpdatedCount: Int,
    val degradedCount: Int,
    val failedCount: Int,
    val failureSamples: String?,
    override val errorMessage: String?,
    override val endedAt: LocalDateTime,
) : UpdateCrawlerRunCommand

data class FailCrawlerRunCommand(
    override val runId: Long,
    override val errorMessage: String?,
    override val endedAt: LocalDateTime,
    override val status: CrawlerRunStatusResult = CrawlerRunStatusResult.FAILED,
) : UpdateCrawlerRunCommand

data class CrawlerRunState(
    val id: Long,
    val status: CrawlerRunStatusResult,
)

data class CrawlerRunRecord(
    val id: Long,
    val status: CrawlerRunStatusResult,
)

enum class CrawlerRunStatusResult {
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED_LOCKED,
}

enum class CrawlerRunCompletionStatus {
    SUCCEEDED,
    FAILED,
}

data class CrawlerRunSummary(
    val processedCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val detailRefreshCount: Int,
    val priceOnlyUpdatedCount: Int,
    val degradedCount: Int,
    val failedCount: Int,
    val failureSamples: List<String>,
)
