package going9.laptopgg.job.crawler

import going9.laptopgg.application.crawler.SaveResult

internal class CrawlProgress(
    private val maxSampleCount: Int = MAX_SAMPLE_COUNT,
) {
    var processedCount: Int = 0
        private set
    var createdCount: Int = 0
        private set
    var updatedCount: Int = 0
        private set
    var degradedCount: Int = 0
        private set
    var priceOnlyUpdatedCount: Int = 0
        private set
    var detailRefreshCount: Int = 0
        private set
    var failedCount: Int = 0
        private set

    private val degradedSamples = mutableListOf<String>()
    private val failureSamples = mutableListOf<String>()

    fun remainingQuota(limit: Int?): Int? {
        return limit?.let { (it - processedCount).coerceAtLeast(0) }
    }

    fun recordProcessed(count: Int) {
        processedCount += count
    }

    fun recordDetailRefresh(count: Int) {
        detailRefreshCount += count
    }

    fun recordSaveResult(result: SaveResult) {
        when (result) {
            SaveResult.CREATED -> createdCount++
            SaveResult.UPDATED -> updatedCount++
            SaveResult.UNCHANGED -> Unit
        }
    }

    fun recordPriceOnlySaveResult(result: SaveResult): Boolean {
        return when (result) {
            SaveResult.UPDATED -> {
                updatedCount++
                priceOnlyUpdatedCount++
                true
            }
            SaveResult.CREATED, SaveResult.UNCHANGED -> false
        }
    }

    fun recordDegraded(productCard: ProductCard, reason: String) {
        degradedCount++
        recordSample(degradedSamples, productCard, reason)
    }

    fun recordFailure(productCard: ProductCard, reason: String) {
        failedCount++
        recordSample(failureSamples, productCard, reason)
    }

    fun reachedLimit(limit: Int?): Boolean {
        return limit != null && processedCount >= limit
    }

    fun toSummary(): CrawlSummary {
        return CrawlSummary(
            processedCount = processedCount,
            createdCount = createdCount,
            updatedCount = updatedCount,
            degradedCount = degradedCount,
            degradedSamples = degradedSamples.toList(),
            failedCount = failedCount,
            failureSamples = failureSamples.toList(),
        )
    }

    private fun recordSample(
        samples: MutableList<String>,
        productCard: ProductCard,
        reason: String,
    ) {
        if (samples.size >= maxSampleCount) {
            return
        }

        samples += "${productCard.productCode} | ${productCard.productName} | $reason"
    }

    companion object {
        private const val MAX_SAMPLE_COUNT = 10
    }
}
