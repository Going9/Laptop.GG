package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledListSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawledListSnapshotProjection
import going9.laptopgg.infrastructure.jpa.repository.crawler.ExistingCrawledLaptopProjection
import going9.laptopgg.persistence.model.laptop.Laptop

internal fun Laptop.toPersistedCrawledLaptopSnapshot(): PersistedCrawledLaptopSnapshot {
    return PersistedCrawledLaptopSnapshot(
        id = id ?: throw CrawlerInvalidStateException("Persisted laptop id must not be null."),
        name = name,
        imageUrl = imageUrl,
        detailPage = detailPage,
        productCode = productCode,
        price = price,
        cpuManufacturer = cpuManufacturer,
        cpu = cpu,
        os = os,
        screenSize = screenSize,
        resolution = resolution,
        brightness = brightness,
        refreshRate = refreshRate,
        ramSize = ramSize,
        ramType = ramType,
        isRamReplaceable = isRamReplaceable,
        graphicsType = graphicsType,
        tgp = tgp,
        thunderboltCount = thunderboltCount,
        usbCCount = usbCCount,
        usbACount = usbACount,
        sdCard = sdCard,
        isSupportsPdCharging = isSupportsPdCharging,
        batteryCapacity = batteryCapacity,
        storageCapacity = storageCapacity,
        storageSlotCount = storageSlotCount,
        weight = weight,
        lastDetailedCrawledAt = lastDetailedCrawledAt,
        usages = laptopUsage.map { usage -> usage.usage },
    )
}

internal fun CrawledListSnapshotProjection.toPersistedCrawledListSnapshot(): PersistedCrawledListSnapshot {
    return PersistedCrawledListSnapshot(
        id = id ?: throw CrawlerInvalidStateException("Persisted laptop id must not be null."),
        name = name,
        imageUrl = imageUrl,
        detailPage = detailPage,
        productCode = productCode,
        price = price,
    )
}

internal fun ExistingCrawledLaptopProjection.toExistingCrawledLaptopSnapshot(): ExistingCrawledLaptopSnapshot {
    return ExistingCrawledLaptopSnapshot(
        id = id ?: throw CrawlerInvalidStateException("Persisted laptop id must not be null."),
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
        usageCount = (usageCount ?: throw CrawlerInvalidStateException("Persisted laptop usage count must not be null.")).toInt(),
    )
}
