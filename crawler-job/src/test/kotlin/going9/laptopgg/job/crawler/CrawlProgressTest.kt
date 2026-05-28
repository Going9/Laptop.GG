package going9.laptopgg.job.crawler

import going9.laptopgg.application.crawler.persistence.SaveResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlProgressTest {
    @Test
    fun `progress accumulates summary counts`() {
        val progress = CrawlProgress(maxSampleCount = 2)

        progress.recordProcessed(3)
        progress.recordDetailRefresh(2)
        progress.recordSaveResult(SaveResult.CREATED)
        progress.recordSaveResult(SaveResult.UPDATED)
        assertThat(progress.recordPriceOnlySaveResult(SaveResult.UPDATED)).isTrue()
        progress.recordDegraded(productCard("1"), "summary fallback")
        progress.recordFailure(productCard("2"), "detail timeout")

        val summary = progress.toSummary()

        assertThat(progress.remainingQuota(5)).isEqualTo(2)
        assertThat(progress.reachedLimit(3)).isTrue()
        assertThat(summary.processedCount).isEqualTo(3)
        assertThat(summary.createdCount).isEqualTo(1)
        assertThat(summary.updatedCount).isEqualTo(2)
        assertThat(summary.degradedCount).isEqualTo(1)
        assertThat(summary.failedCount).isEqualTo(1)
        assertThat(summary.degradedSamples).containsExactly("1 | Laptop 1 | summary fallback")
        assertThat(summary.failureSamples).containsExactly("2 | Laptop 2 | detail timeout")
    }

    @Test
    fun `progress caps degraded and failure samples`() {
        val progress = CrawlProgress(maxSampleCount = 1)

        progress.recordDegraded(productCard("1"), "first")
        progress.recordDegraded(productCard("2"), "second")
        progress.recordFailure(productCard("3"), "third")
        progress.recordFailure(productCard("4"), "fourth")

        val summary = progress.toSummary()

        assertThat(summary.degradedCount).isEqualTo(2)
        assertThat(summary.failedCount).isEqualTo(2)
        assertThat(summary.degradedSamples).containsExactly("1 | Laptop 1 | first")
        assertThat(summary.failureSamples).containsExactly("3 | Laptop 3 | third")
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
