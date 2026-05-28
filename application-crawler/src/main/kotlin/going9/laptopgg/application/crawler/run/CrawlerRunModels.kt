package going9.laptopgg.application.crawler.run

import java.time.LocalDateTime

data class CrawlerLockResult<T>(
    val acquired: Boolean,
    val value: T?,
)

data class CreateCrawlerRunCommand(
    val filterProfile: String,
    val startPage: Int,
    val limitCount: Int?,
    val startedAt: LocalDateTime,
    val status: CrawlerRunStatusResult = CrawlerRunStatusResult.RUNNING,
    val endedAt: LocalDateTime? = null,
    val errorMessage: String? = null,
)

data class UpdateCrawlerRunCommand(
    val runId: Long,
    val status: CrawlerRunStatusResult,
    val processedCount: Int? = null,
    val createdCount: Int? = null,
    val updatedCount: Int? = null,
    val detailRefreshCount: Int? = null,
    val priceOnlyUpdatedCount: Int? = null,
    val degradedCount: Int? = null,
    val failedCount: Int? = null,
    val failureSamples: String? = null,
    val errorMessage: String? = null,
    val endedAt: LocalDateTime,
)

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
