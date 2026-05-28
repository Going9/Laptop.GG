package going9.laptopgg.application.crawler

interface TrackCrawlerRunUseCase {
    fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord
    fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord
    fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String? = null,
    ): CrawlerRunRecord
    fun fail(runId: Long, exception: Throwable): CrawlerRunRecord
}

data class CrawlerRunRecord(
    val id: Long?,
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
    val degradedCount: Int,
    val failedCount: Int,
    val failureSamples: List<String>,
)
