package going9.laptopgg.job.crawler.danawa.list

import going9.laptopgg.job.crawler.danawa.client.DanawaClient
import going9.laptopgg.job.crawler.list.ListRequestContext
import going9.laptopgg.job.crawler.list.ProductPageBatch
import going9.laptopgg.job.crawler.list.ProductListPageCrawler
import going9.laptopgg.job.crawler.source.CrawlSource
import org.springframework.stereotype.Component

@Component
internal class DanawaListPageCrawler(
    private val danawaClient: DanawaClient,
) : ProductListPageCrawler {
    override fun createListRequestContext(crawlSource: CrawlSource): ListRequestContext {
        val initialListHtml = danawaClient.fetchInitialListPage(crawlSource.listUrl)
        return DanawaListRequestContextParser.extractListRequestContext(initialListHtml, crawlSource)
            .copy(sortMethod = LIST_SORT_METHOD)
    }

    override fun fetchProductPageBatch(page: Int, listRequestContext: ListRequestContext): ProductPageBatch {
        val html = danawaClient.fetchListPage(page, listRequestContext)
        val metadata = DanawaListPageMetadataParser.parse(html, currentPage = page)
        val productCards = DanawaProductCardParser.parse(html)
        require(productCards.isNotEmpty() || metadata.priceCompareCount != null) {
            "Danawa list response has no recognizable products or product count. page=$page"
        }
        return ProductPageBatch(
            productCards = productCards,
            hasNextPage = metadata.hasNextPage,
            priceCompareCount = metadata.priceCompareCount,
            visiblePageNumbers = metadata.visiblePageNumbers,
            nextPageHint = metadata.nextPageHint,
        )
    }

    private companion object {
        const val LIST_SORT_METHOD = "MinPrice"
    }
}
