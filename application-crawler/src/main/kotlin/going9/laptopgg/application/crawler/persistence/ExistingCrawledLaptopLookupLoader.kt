package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort

internal class ExistingCrawledLaptopLookupLoader(
    private val laptopPort: CrawledLaptopPersistencePort,
) {
    fun load(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
        if (productCards.isEmpty()) {
            return ExistingCrawledLaptopLookup(emptyMap(), emptyMap())
        }

        val byProductCode = laptopPort.findAllByProductCodes(productCards.map { it.productCode }.distinct())
            .mapNotNull { laptop -> laptop.productCode?.let { it to laptop.toExistingSnapshot() } }
            .toMap()
        val byDetailPage = laptopPort.findAllByDetailPages(productCards.map { it.detailPage }.distinct())
            .associate { laptop -> laptop.detailPage to laptop.toExistingSnapshot() }

        return ExistingCrawledLaptopLookup(
            byProductCode = byProductCode,
            byDetailPage = byDetailPage,
        )
    }

    private fun PersistedCrawledLaptopSnapshot.toExistingSnapshot(): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = id,
            productCode = productCode,
            detailPage = detailPage,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = os,
            screenSize = screenSize,
            resolution = resolution,
            ramSize = ramSize,
            graphicsType = graphicsType,
            storageCapacity = storageCapacity,
            batteryCapacity = batteryCapacity,
            weight = weight,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usageCount = usages.size,
        )
    }
}
