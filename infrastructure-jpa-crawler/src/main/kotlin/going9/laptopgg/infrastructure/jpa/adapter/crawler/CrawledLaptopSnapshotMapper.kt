package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.persistence.model.laptop.Laptop

internal fun Laptop.toPersistedCrawledLaptopSnapshot(): PersistedCrawledLaptopSnapshot {
    return PersistedCrawledLaptopSnapshot(
        id = requireNotNull(id) { "Persisted laptop id must not be null." },
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
