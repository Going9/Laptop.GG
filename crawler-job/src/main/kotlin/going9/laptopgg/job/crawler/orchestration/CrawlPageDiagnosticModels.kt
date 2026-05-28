package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.ProductPageBatch

internal data class CrawlPageDiagnosticContext(
    val sourceKey: String,
    val page: Int,
    val pageBatch: ProductPageBatch,
    val productCards: List<ProductCard>,
    val expectedLastPage: Int?,
    val repeatedPageSignature: Boolean,
    val pageSignature: String,
    val requestSortMethod: String,
    val requestFilterCount: Int,
    val requestDistinctFilterCount: Int,
)

internal data class FormattedCrawlPageDiagnostics(
    val visiblePages: String,
    val nextPageHint: String,
    val priceCompareCount: String,
    val expectedLastPage: String,
    val pageSignatureHash: String,
    val firstCard: String,
    val lastCard: String,
)
