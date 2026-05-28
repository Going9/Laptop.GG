package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.ProductPageBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlPageDiagnosticFormatterTest {
    private val formatter = CrawlPageDiagnosticFormatter()

    @Test
    fun `formats page metadata and product card labels`() {
        val context = diagnosticContext(
            pageBatch = productPageBatch(
                priceCompareCount = 42,
                visiblePageNumbers = listOf(1, 2, 3),
                nextPageHint = 4,
            ),
            productCards = listOf(
                productCard(productCode = "A100", detailPage = "https://prod.danawa.com/info/?pcode=A100&cate=112758"),
                productCard(productCode = "B200", detailPage = "https://prod.danawa.com/info/?pcode=B200"),
            ),
            expectedLastPage = 5,
            pageSignature = "signature",
        )

        val diagnostics = formatter.format(context)

        assertThat(diagnostics.visiblePages).isEqualTo("1,2,3")
        assertThat(diagnostics.nextPageHint).isEqualTo("4")
        assertThat(diagnostics.priceCompareCount).isEqualTo("42")
        assertThat(diagnostics.expectedLastPage).isEqualTo("5")
        assertThat(diagnostics.pageSignatureHash).isEqualTo(ProductPageSignature.stableHash("signature"))
        assertThat(diagnostics.firstCard).isEqualTo("A100@112758")
        assertThat(diagnostics.lastCard).isEqualTo("B200@112758")
    }

    @Test
    fun `formats missing metadata with operation friendly placeholders`() {
        val diagnostics = formatter.format(
            diagnosticContext(
                pageBatch = productPageBatch(
                    priceCompareCount = null,
                    visiblePageNumbers = emptyList(),
                    nextPageHint = null,
                ),
                productCards = emptyList(),
                expectedLastPage = null,
                pageSignature = "",
            ),
        )

        assertThat(diagnostics.visiblePages).isEqualTo("없음")
        assertThat(diagnostics.nextPageHint).isEqualTo("없음")
        assertThat(diagnostics.priceCompareCount).isEqualTo("알 수 없음")
        assertThat(diagnostics.expectedLastPage).isEqualTo("알 수 없음")
        assertThat(diagnostics.firstCard).isEqualTo("없음")
        assertThat(diagnostics.lastCard).isEqualTo("없음")
    }

    private fun diagnosticContext(
        pageBatch: ProductPageBatch,
        productCards: List<ProductCard>,
        expectedLastPage: Int?,
        pageSignature: String,
    ): CrawlPageDiagnosticContext {
        return CrawlPageDiagnosticContext(
            sourceKey = "source",
            page = 1,
            pageBatch = pageBatch,
            productCards = productCards,
            expectedLastPage = expectedLastPage,
            repeatedPageSignature = false,
            pageSignature = pageSignature,
            requestSortMethod = "NEW",
            requestFilterCount = 2,
            requestDistinctFilterCount = 2,
        )
    }

    private fun productPageBatch(
        priceCompareCount: Int?,
        visiblePageNumbers: List<Int>,
        nextPageHint: Int?,
    ): ProductPageBatch {
        return ProductPageBatch(
            productCards = emptyList(),
            hasNextPage = true,
            priceCompareCount = priceCompareCount,
            visiblePageNumbers = visiblePageNumbers,
            nextPageHint = nextPageHint,
        )
    }

    private fun productCard(productCode: String, detailPage: String): ProductCard {
        return ProductCard(
            productCode = productCode,
            productName = "Laptop $productCode",
            detailPage = detailPage,
            imageUrl = "https://img.danawa.com/$productCode.jpg",
            price = 1_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }
}
