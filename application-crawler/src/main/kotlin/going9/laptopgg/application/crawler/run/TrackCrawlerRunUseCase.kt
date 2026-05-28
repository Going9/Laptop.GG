package going9.laptopgg.application.crawler.run

interface TrackCrawlerRunUseCase {
    fun start(filterProfile: CrawlerFilterProfile, startPage: Int, limit: Int?): CrawlerRunRecord
    fun skipLocked(filterProfile: CrawlerFilterProfile, startPage: Int, limit: Int?): CrawlerRunRecord
    fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String? = null,
    ): CrawlerRunRecord
    fun fail(runId: Long, failure: Throwable): CrawlerRunRecord
}
