package going9.laptopgg.job.crawler.detail

import java.util.concurrent.ExecutorService

internal interface ProductDetailCrawler {
    fun fetchDetailRefreshOutcomes(
        workItems: List<DetailRefreshWorkItem>,
        executor: ExecutorService,
    ): List<DetailRefreshOutcome>
}
