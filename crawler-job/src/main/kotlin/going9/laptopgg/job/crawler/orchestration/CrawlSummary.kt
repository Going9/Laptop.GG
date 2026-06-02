package going9.laptopgg.job.crawler.orchestration

internal data class CrawlSummary(
    val processedCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val detailRefreshCount: Int,
    val priceOnlyUpdatedCount: Int,
    val degradedCount: Int,
    val degradedSamples: List<String>,
    val failedCount: Int,
    val failureSamples: List<String>,
)

internal class CrawlFailedWithPartialSummary(
    val partialSummary: CrawlSummary,
    cause: Exception,
) : RuntimeException(cause.message ?: cause::class.simpleName ?: "Crawler failed.", cause)
