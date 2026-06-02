package going9.laptopgg.job.crawler.detail

internal interface ProductDetailCrawler {
    fun fetchDetailRefreshOutcomes(
        workItems: List<DetailRefreshWorkItem>,
        detailFetchExecutor: DetailFetchExecutor,
    ): List<DetailRefreshOutcome>
}
