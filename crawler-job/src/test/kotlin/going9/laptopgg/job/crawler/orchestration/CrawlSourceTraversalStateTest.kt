package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.ProductPageBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlSourceTraversalStateTest {
    @Test
    fun `analysis separates fresh and duplicate product cards while tracking page signatures`() {
        val state = CrawlSourceTraversalState(startPage = 0, seenDetailPages = mutableSetOf())
        val firstBatch = productPageBatch(listOf(productCard("1"), productCard("2")), priceCompareCount = 61)

        val firstAnalysis = state.analyze(firstBatch)
        val repeatedAnalysis = state.analyze(firstBatch)

        assertThat(state.currentPage).isEqualTo(1)
        assertThat(firstAnalysis.expectedLastPage).isEqualTo(31)
        assertThat(firstAnalysis.repeatedPageSignature).isFalse()
        assertThat(firstAnalysis.freshProductCards.map { it.productCode }).containsExactly("1", "2")
        assertThat(firstAnalysis.duplicateSkippedCount).isZero()
        assertThat(firstAnalysis.consecutiveDuplicateOnlyPages).isZero()
        assertThat(repeatedAnalysis.repeatedPageSignature).isTrue()
        assertThat(repeatedAnalysis.freshProductCards).isEmpty()
        assertThat(repeatedAnalysis.duplicateSkippedCount).isEqualTo(2)
        assertThat(repeatedAnalysis.consecutiveDuplicateOnlyPages).isEqualTo(1)
    }

    @Test
    fun `run result reflects limit and max page state`() {
        val state = CrawlSourceTraversalState(startPage = 2, seenDetailPages = mutableSetOf())

        state.markReachedLimit()
        state.advance()

        assertThat(state.toRunResult(maxListPages = 2))
            .isEqualTo(CrawlSourceRunResult(reachedLimit = true, hitMaxListPages = true))
    }

    private fun productPageBatch(
        productCards: List<ProductCard>,
        priceCompareCount: Int?,
    ): ProductPageBatch {
        return ProductPageBatch(
            productCards = productCards,
            hasNextPage = true,
            priceCompareCount = priceCompareCount,
            visiblePageNumbers = emptyList(),
            nextPageHint = null,
        )
    }

    private fun productCard(code: String): ProductCard {
        return ProductCard(
            productCode = code,
            productName = "Laptop $code",
            detailPage = "https://prod.danawa.com/info/?pcode=$code&cate=112758",
            imageUrl = "https://img.danawa.com/$code.jpg",
            price = 1_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }
}
