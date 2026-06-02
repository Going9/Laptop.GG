package going9.laptopgg.job.crawler.danawa.detail

import going9.laptopgg.job.crawler.danawa.client.DanawaClient
import going9.laptopgg.job.crawler.danawa.client.DanawaHttpInterruptedException
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import going9.laptopgg.job.crawler.detail.DetailRefreshWorkItem
import going9.laptopgg.job.crawler.list.ProductCard
import java.net.URI
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class DanawaDetailCrawlerInterruptionTest {
    @Test
    fun `interrupted detail fetch is propagated instead of converted to product failure`() {
        val productCard = productCard("100")
        val failure = DanawaHttpInterruptedException(
            URI.create(productCard.detailPage),
            InterruptedException("sleep interrupted"),
        )
        val danawaClient = Mockito.mock(DanawaClient::class.java)
        Mockito.`when`(danawaClient.fetchDetailPage(productCard.detailPage)).thenThrow(failure)
        val crawler = DanawaDetailCrawler(
            danawaClient = danawaClient,
            summaryFallbackParser = Mockito.mock(DanawaSummaryFallbackParser::class.java),
            laptopSnapshotMerger = Mockito.mock(LaptopSnapshotMerger::class.java),
        )

        try {
            DetailFetchExecutor.fixed(1).use { detailFetchExecutor ->
                assertThatThrownBy {
                    crawler.fetchDetailRefreshOutcomes(
                        workItems = listOf(DetailRefreshWorkItem(productCard = productCard, existingLaptop = null)),
                        detailFetchExecutor = detailFetchExecutor,
                    )
                }.isSameAs(failure)
            }
            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }
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
