package going9.laptopgg.application.crawler.persistence

internal class CrawledLaptopSnapshotPatcher {
    fun applyDetailUpdate(
        existingLaptop: PersistedCrawledLaptopSnapshot,
        updateCommand: UpdateCrawledLaptopCommand,
    ): PersistedCrawledLaptopSnapshot {
        return existingLaptop.copy(
            name = updateCommand.name ?: existingLaptop.name,
            imageUrl = updateCommand.imageUrl ?: existingLaptop.imageUrl,
            detailPage = updateCommand.detailPage ?: existingLaptop.detailPage,
            productCode = updateCommand.productCode ?: existingLaptop.productCode,
            price = updateCommand.price ?: existingLaptop.price,
            cpuManufacturer = updateCommand.cpuManufacturer ?: existingLaptop.cpuManufacturer,
            cpu = updateCommand.cpu ?: existingLaptop.cpu,
            os = updateCommand.os ?: existingLaptop.os,
            screenSize = updateCommand.screenSize ?: existingLaptop.screenSize,
            resolution = updateCommand.resolution ?: existingLaptop.resolution,
            brightness = updateCommand.brightness ?: existingLaptop.brightness,
            refreshRate = updateCommand.refreshRate ?: existingLaptop.refreshRate,
            ramSize = updateCommand.ramSize ?: existingLaptop.ramSize,
            ramType = updateCommand.ramType ?: existingLaptop.ramType,
            isRamReplaceable = updateCommand.isRamReplaceable ?: existingLaptop.isRamReplaceable,
            graphicsType = updateCommand.graphicsType ?: existingLaptop.graphicsType,
            tgp = updateCommand.tgp ?: existingLaptop.tgp,
            thunderboltCount = updateCommand.thunderboltCount ?: existingLaptop.thunderboltCount,
            usbCCount = updateCommand.usbCCount ?: existingLaptop.usbCCount,
            usbACount = updateCommand.usbACount ?: existingLaptop.usbACount,
            sdCard = updateCommand.sdCard ?: existingLaptop.sdCard,
            isSupportsPdCharging = updateCommand.isSupportsPdCharging ?: existingLaptop.isSupportsPdCharging,
            batteryCapacity = updateCommand.batteryCapacity ?: existingLaptop.batteryCapacity,
            storageCapacity = updateCommand.storageCapacity ?: existingLaptop.storageCapacity,
            storageSlotCount = updateCommand.storageSlotCount ?: existingLaptop.storageSlotCount,
            weight = updateCommand.weight ?: existingLaptop.weight,
            lastDetailedCrawledAt = updateCommand.lastDetailedCrawledAt ?: existingLaptop.lastDetailedCrawledAt,
            usages = updateCommand.usages ?: existingLaptop.usages,
        )
    }
}
