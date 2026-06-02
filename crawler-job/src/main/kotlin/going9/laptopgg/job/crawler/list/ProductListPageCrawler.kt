package going9.laptopgg.job.crawler.list

import going9.laptopgg.job.crawler.source.CrawlSource

internal interface ProductListPageCrawler {
    fun createListRequestContext(crawlSource: CrawlSource): ListRequestContext

    fun fetchProductPageBatch(page: Int, listRequestContext: ListRequestContext): ProductPageBatch
}
