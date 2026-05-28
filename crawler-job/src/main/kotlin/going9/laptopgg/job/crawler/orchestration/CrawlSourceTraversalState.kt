package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.ProductPageBatch

internal class CrawlSourceTraversalState(
    startPage: Int,
    private val seenDetailPages: MutableSet<String>,
) {
    private val seenPageSignatures = linkedSetOf<String>()
    private var consecutiveDuplicateOnlyPages = 0

    var currentPage: Int = startPage.coerceAtLeast(1)
        private set

    var reachedLimit: Boolean = false
        private set

    fun analyze(pageBatch: ProductPageBatch): CrawlPageAnalysis {
        val productCards = pageBatch.productCards
        val pageSignature = ProductPageSignature.create(productCards)
        val repeatedPageSignature = !seenPageSignatures.add(pageSignature)
        val freshProductCards = productCards.filter { seenDetailPages.add(it.detailPage) }

        consecutiveDuplicateOnlyPages = if (freshProductCards.isEmpty()) {
            consecutiveDuplicateOnlyPages + 1
        } else {
            0
        }

        return CrawlPageAnalysis(
            productCards = productCards,
            expectedLastPage = pageBatch.expectedLastPage(),
            pageSignature = pageSignature,
            repeatedPageSignature = repeatedPageSignature,
            freshProductCards = freshProductCards,
            duplicateSkippedCount = productCards.size - freshProductCards.size,
            consecutiveDuplicateOnlyPages = consecutiveDuplicateOnlyPages,
        )
    }

    fun markReachedLimit() {
        reachedLimit = true
    }

    fun advance() {
        currentPage++
    }

    fun toRunResult(maxListPages: Int): CrawlSourceRunResult {
        return CrawlSourceRunResult(
            reachedLimit = reachedLimit,
            hitMaxListPages = currentPage > maxListPages,
        )
    }

    private fun ProductPageBatch.expectedLastPage(): Int? {
        return priceCompareCount
            ?.takeIf { it > 0 }
            ?.let { ((it - 1) / productCards.size.coerceAtLeast(1)) + 1 }
    }
}

internal data class CrawlPageAnalysis(
    val productCards: List<ProductCard>,
    val expectedLastPage: Int?,
    val pageSignature: String,
    val repeatedPageSignature: Boolean,
    val freshProductCards: List<ProductCard>,
    val duplicateSkippedCount: Int,
    val consecutiveDuplicateOnlyPages: Int,
)
