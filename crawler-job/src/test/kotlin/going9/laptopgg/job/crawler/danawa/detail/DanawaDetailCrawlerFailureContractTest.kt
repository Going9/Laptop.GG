package going9.laptopgg.job.crawler.danawa.detail

import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.job.crawler.danawa.client.DanawaClient
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import going9.laptopgg.job.crawler.detail.DetailRefreshWorkItem
import going9.laptopgg.job.crawler.list.ProductCard
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class DanawaDetailCrawlerFailureContractTest {
    private val danawaClient = Mockito.mock(DanawaClient::class.java)
    private val crawler = DanawaDetailCrawler(
        danawaClient = danawaClient,
        summaryFallbackParser = DanawaSummaryFallbackParser(CrawledCpuManufacturerResolver()),
        laptopSnapshotMerger = LaptopSnapshotMerger(
            crawledCpuManufacturerResolver = CrawledCpuManufacturerResolver(),
            crawledCpuModelResolver = CrawledCpuModelResolver(),
            crawledGraphicsModelResolver = CrawledGraphicsModelResolver(),
        ),
    )

    @Test
    fun `detail fetch exception is returned as product failure outcome`() {
        val workItem = detailWorkItem("100")
        val exception = RuntimeException("detail timeout")
        Mockito.`when`(danawaClient.fetchDetailPage(workItem.productCard.detailPage)).thenThrow(exception)

        DetailFetchExecutor.fixed(1).use { executor ->
            val outcomes = crawler.fetchDetailRefreshOutcomes(listOf(workItem), executor)

            assertThat(outcomes).hasSize(1)
            assertThat(outcomes.first().workItem).isEqualTo(workItem)
            assertThat(outcomes.first().buildResult).isNull()
            assertThat(outcomes.first().error).isSameAs(exception)
        }
    }

    @Test
    fun `fatal detail fetch error is not downgraded to product failure outcome`() {
        val workItem = detailWorkItem("200")
        val error = NoClassDefFoundError("danawa parser linkage")
        Mockito.`when`(danawaClient.fetchDetailPage(workItem.productCard.detailPage)).thenThrow(error)

        assertThatThrownBy {
            DetailFetchExecutor.fixed(1).use { executor ->
                crawler.fetchDetailRefreshOutcomes(listOf(workItem), executor)
            }
        }.isSameAs(error)
    }

    private fun detailWorkItem(code: String): DetailRefreshWorkItem {
        return DetailRefreshWorkItem(
            productCard = ProductCard(
                productCode = code,
                productName = "Laptop $code",
                detailPage = "https://prod.danawa.com/info/?pcode=$code&cate=112758",
                imageUrl = "https://img.danawa.com/$code.jpg",
                price = 1_000,
                cate1 = "112",
                cate2 = "758",
                cate3 = "0",
                cate4 = "112758",
            ),
            existingLaptop = null,
        )
    }
}
