package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.ProductPageBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlSourceStopPolicyTest {
    @Test
    fun `reached limit wins over other stop reasons`() {
        val decision = CrawlSourceStopPolicy.decideAfterProcessedPage(
            pageBatch = productPageBatch(hasNextPage = false),
            pageAnalysis = analysis(expectedLastPage = 1),
            currentPage = 1,
            reachedLimit = true,
        )

        assertThat(decision.reason).isEqualTo(CrawlSourceStopReason.REACHED_LIMIT)
    }

    @Test
    fun `expected last page stops before next page hint`() {
        val decision = CrawlSourceStopPolicy.decideAfterProcessedPage(
            pageBatch = productPageBatch(hasNextPage = true),
            pageAnalysis = analysis(expectedLastPage = 3),
            currentPage = 3,
            reachedLimit = false,
        )

        assertThat(decision.reason).isEqualTo(CrawlSourceStopReason.EXPECTED_LAST_PAGE)
    }

    @Test
    fun `duplicate tail and missing next page are explicit stop reasons`() {
        val duplicateDecision = CrawlSourceStopPolicy.decideAfterProcessedPage(
            pageBatch = productPageBatch(hasNextPage = true),
            pageAnalysis = analysis(freshProductCards = emptyList(), consecutiveDuplicateOnlyPages = 5),
            currentPage = 2,
            reachedLimit = false,
        )
        val noNextPageDecision = CrawlSourceStopPolicy.decideAfterProcessedPage(
            pageBatch = productPageBatch(hasNextPage = false),
            pageAnalysis = analysis(),
            currentPage = 2,
            reachedLimit = false,
        )

        assertThat(duplicateDecision.reason).isEqualTo(CrawlSourceStopReason.DUPLICATE_TAIL)
        assertThat(noNextPageDecision.reason).isEqualTo(CrawlSourceStopReason.NO_NEXT_PAGE)
    }

    @Test
    fun `continues when no stop condition matches`() {
        val decision = CrawlSourceStopPolicy.decideAfterProcessedPage(
            pageBatch = productPageBatch(hasNextPage = true),
            pageAnalysis = analysis(),
            currentPage = 2,
            reachedLimit = false,
        )

        assertThat(decision.shouldStop).isFalse()
    }

    private fun productPageBatch(hasNextPage: Boolean): ProductPageBatch {
        return ProductPageBatch(
            productCards = listOf(productCard("1")),
            hasNextPage = hasNextPage,
            priceCompareCount = null,
            visiblePageNumbers = emptyList(),
            nextPageHint = null,
        )
    }

    private fun analysis(
        expectedLastPage: Int? = null,
        freshProductCards: List<ProductCard> = listOf(productCard("1")),
        consecutiveDuplicateOnlyPages: Int = 0,
    ): CrawlPageAnalysis {
        return CrawlPageAnalysis(
            productCards = listOf(productCard("1")),
            expectedLastPage = expectedLastPage,
            pageSignature = "signature",
            repeatedPageSignature = false,
            freshProductCards = freshProductCards,
            duplicateSkippedCount = 0,
            consecutiveDuplicateOnlyPages = consecutiveDuplicateOnlyPages,
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
