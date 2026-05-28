package going9.laptopgg.job.crawler.orchestration

data class CrawlSummary(
    val processedCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val degradedCount: Int,
    val degradedSamples: List<String>,
    val failedCount: Int,
    val failureSamples: List<String>,
)
