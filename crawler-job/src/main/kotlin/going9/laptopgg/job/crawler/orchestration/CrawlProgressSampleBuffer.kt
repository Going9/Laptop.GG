package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard

internal class CrawlProgressSampleBuffer(
    private val maxSampleCount: Int,
) {
    private val samples = mutableListOf<String>()

    fun record(productCard: ProductCard, reason: String) {
        if (samples.size >= maxSampleCount) {
            return
        }

        samples += formatSample(productCard, reason)
    }

    fun toList(): List<String> {
        return samples.toList()
    }

    private fun formatSample(productCard: ProductCard, reason: String): String {
        return "${productCard.productCode} | ${productCard.productName} | $reason"
    }
}
