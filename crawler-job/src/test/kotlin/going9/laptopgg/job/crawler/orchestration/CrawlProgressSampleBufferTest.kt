package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlProgressSampleBufferTest {
    @Test
    fun `sample buffer formats and caps product samples`() {
        val buffer = CrawlProgressSampleBuffer(maxSampleCount = 1)

        buffer.record(productCard("1"), "first")
        buffer.record(productCard("2"), "second")

        assertThat(buffer.toList()).containsExactly("1 | Laptop 1 | first")
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
