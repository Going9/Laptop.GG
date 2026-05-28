package going9.laptopgg.application.crawler.persistence

internal class CrawledLaptopChangeDetector(
    private val fieldChangePolicy: CrawledLaptopFieldChangePolicy = CrawledLaptopFieldChangePolicy(),
) {
    fun listSnapshotUpdate(
        existingLaptop: PersistedCrawledLaptopSnapshot,
        productCard: CrawledProductCardCommand,
    ): UpdateCrawledLaptopCommand {
        return UpdateCrawledLaptopCommand(
            name = fieldChangePolicy.changedText(existingLaptop.name, productCard.productName),
            imageUrl = fieldChangePolicy.changedText(existingLaptop.imageUrl, productCard.imageUrl),
            detailPage = fieldChangePolicy.changedText(existingLaptop.detailPage, productCard.detailPage),
            productCode = fieldChangePolicy.changedText(existingLaptop.productCode, productCard.productCode),
            price = fieldChangePolicy.changedPresent(existingLaptop.price, productCard.price),
        )
    }

    fun detailUpdate(
        existingLaptop: PersistedCrawledLaptopSnapshot,
        command: CrawledLaptopCommand,
    ): UpdateCrawledLaptopCommand {
        return UpdateCrawledLaptopCommand(
            name = fieldChangePolicy.changedText(existingLaptop.name, command.name),
            imageUrl = fieldChangePolicy.changedText(existingLaptop.imageUrl, command.imageUrl),
            detailPage = fieldChangePolicy.changedText(existingLaptop.detailPage, command.detailPage),
            productCode = fieldChangePolicy.changedText(existingLaptop.productCode, command.productCode),
            price = fieldChangePolicy.changedPresent(existingLaptop.price, command.price),
            cpuManufacturer = fieldChangePolicy.changedText(existingLaptop.cpuManufacturer, command.cpuManufacturer),
            cpu = fieldChangePolicy.changedText(existingLaptop.cpu, command.cpu),
            os = fieldChangePolicy.changedText(existingLaptop.os, command.os),
            screenSize = fieldChangePolicy.changedPresent(existingLaptop.screenSize, command.screenSize),
            resolution = fieldChangePolicy.changedText(existingLaptop.resolution, command.resolution),
            brightness = fieldChangePolicy.changedPresent(existingLaptop.brightness, command.brightness),
            refreshRate = fieldChangePolicy.changedPresent(existingLaptop.refreshRate, command.refreshRate),
            ramSize = fieldChangePolicy.changedPresent(existingLaptop.ramSize, command.ramSize),
            ramType = fieldChangePolicy.changedText(existingLaptop.ramType, command.ramType),
            isRamReplaceable = fieldChangePolicy.changedPresent(existingLaptop.isRamReplaceable, command.isRamReplaceable),
            graphicsType = fieldChangePolicy.changedText(existingLaptop.graphicsType, command.graphicsType),
            tgp = fieldChangePolicy.changedPresent(existingLaptop.tgp, command.tgp),
            thunderboltCount = fieldChangePolicy.changedPresent(existingLaptop.thunderboltCount, command.thunderboltCount),
            usbCCount = fieldChangePolicy.changedPresent(existingLaptop.usbCCount, command.usbCCount),
            usbACount = fieldChangePolicy.changedPresent(existingLaptop.usbACount, command.usbACount),
            sdCard = fieldChangePolicy.changedText(existingLaptop.sdCard, command.sdCard),
            isSupportsPdCharging = fieldChangePolicy.changedPresent(existingLaptop.isSupportsPdCharging, command.isSupportsPdCharging),
            batteryCapacity = fieldChangePolicy.changedPresent(existingLaptop.batteryCapacity, command.batteryCapacity),
            storageCapacity = fieldChangePolicy.changedPresent(existingLaptop.storageCapacity, command.storageCapacity),
            storageSlotCount = fieldChangePolicy.changedPresent(existingLaptop.storageSlotCount, command.storageSlotCount),
            weight = fieldChangePolicy.changedPresent(existingLaptop.weight, command.weight),
            lastDetailedCrawledAt = fieldChangePolicy.changedPresent(existingLaptop.lastDetailedCrawledAt, command.lastDetailedCrawledAt),
            usages = fieldChangePolicy.changedUsages(existingLaptop.usages, command.usages),
        )
    }

    fun hasChanges(updateCommand: UpdateCrawledLaptopCommand): Boolean {
        return fieldChangePolicy.hasChanges(updateCommand)
    }
}
