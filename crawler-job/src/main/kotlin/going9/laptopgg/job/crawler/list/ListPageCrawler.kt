package going9.laptopgg.job.crawler.list

import going9.laptopgg.job.crawler.client.DanawaClient
import going9.laptopgg.job.crawler.source.CrawlSource
import org.springframework.stereotype.Component

@Component
class ListPageCrawler(
    private val danawaClient: DanawaClient,
) {
    internal fun createListRequestContext(crawlSource: CrawlSource): ListRequestContext {
        val initialListHtml = danawaClient.fetchInitialListPage(crawlSource.listUrl)
        return DanawaListParser.extractListRequestContext(initialListHtml, crawlSource)
            .copy(sortMethod = LIST_SORT_METHOD)
    }

    internal fun fetchProductPageBatch(page: Int, listRequestContext: ListRequestContext): ProductPageBatch {
        val html = danawaClient.fetchListPage(page, listRequestContext)
        return ProductPageBatch(
            productCards = DanawaListParser.parseListPage(html),
            hasNextPage = DanawaListParser.hasNextPage(html, page),
            priceCompareCount = DanawaListParser.extractPriceCompareCount(html),
            visiblePageNumbers = DanawaListParser.extractVisiblePageNumbers(html),
            nextPageHint = DanawaListParser.extractNextPageHint(html),
        )
    }

    private companion object {
        const val LIST_SORT_METHOD = "MinPrice"
    }
}
