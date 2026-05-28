package going9.laptopgg.application.crawler

import going9.laptopgg.domain.crawler.CrawlerRun
import going9.laptopgg.domain.crawler.CrawlerRunStatus

interface TrackCrawlerRunUseCase {
    fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRun
    fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRun
    fun finish(runId: Long, summary: CrawlerRunSummary, status: CrawlerRunStatus, errorMessage: String? = null): CrawlerRun
    fun fail(runId: Long, exception: Throwable): CrawlerRun
}

data class CrawlerRunSummary(
    val processedCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val degradedCount: Int,
    val failedCount: Int,
    val failureSamples: List<String>,
)
