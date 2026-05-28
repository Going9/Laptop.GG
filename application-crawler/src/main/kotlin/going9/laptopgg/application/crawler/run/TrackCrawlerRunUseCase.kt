package going9.laptopgg.application.crawler.run

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
