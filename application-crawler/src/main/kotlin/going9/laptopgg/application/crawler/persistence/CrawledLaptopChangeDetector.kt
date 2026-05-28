package going9.laptopgg.application.crawler.persistence

class CrawledLaptopChangeDetector {
    fun listSnapshotUpdate(
        existingLaptop: PersistedCrawledLaptopSnapshot,
        productCard: CrawledProductCardCommand,
    ): UpdateCrawledLaptopCommand {
        return UpdateCrawledLaptopCommand(
            name = changedText(existingLaptop.name, productCard.productName),
            imageUrl = changedText(existingLaptop.imageUrl, productCard.imageUrl),
            detailPage = changedText(existingLaptop.detailPage, productCard.detailPage),
            productCode = changedText(existingLaptop.productCode, productCard.productCode),
            price = changedPresent(existingLaptop.price, productCard.price),
        )
    }

    fun detailUpdate(
        existingLaptop: PersistedCrawledLaptopSnapshot,
        command: CrawledLaptopCommand,
    ): UpdateCrawledLaptopCommand {
        return UpdateCrawledLaptopCommand(
            name = changedText(existingLaptop.name, command.name),
            imageUrl = changedText(existingLaptop.imageUrl, command.imageUrl),
            detailPage = changedText(existingLaptop.detailPage, command.detailPage),
            productCode = changedText(existingLaptop.productCode, command.productCode),
            price = changedPresent(existingLaptop.price, command.price),
            cpuManufacturer = changedText(existingLaptop.cpuManufacturer, command.cpuManufacturer),
            cpu = changedText(existingLaptop.cpu, command.cpu),
            os = changedText(existingLaptop.os, command.os),
            screenSize = changedPresent(existingLaptop.screenSize, command.screenSize),
            resolution = changedText(existingLaptop.resolution, command.resolution),
            brightness = changedPresent(existingLaptop.brightness, command.brightness),
            refreshRate = changedPresent(existingLaptop.refreshRate, command.refreshRate),
            ramSize = changedPresent(existingLaptop.ramSize, command.ramSize),
            ramType = changedText(existingLaptop.ramType, command.ramType),
            isRamReplaceable = changedPresent(existingLaptop.isRamReplaceable, command.isRamReplaceable),
            graphicsType = changedText(existingLaptop.graphicsType, command.graphicsType),
            tgp = changedPresent(existingLaptop.tgp, command.tgp),
            thunderboltCount = changedPresent(existingLaptop.thunderboltCount, command.thunderboltCount),
            usbCCount = changedPresent(existingLaptop.usbCCount, command.usbCCount),
            usbACount = changedPresent(existingLaptop.usbACount, command.usbACount),
            sdCard = changedText(existingLaptop.sdCard, command.sdCard),
            isSupportsPdCharging = changedPresent(existingLaptop.isSupportsPdCharging, command.isSupportsPdCharging),
            batteryCapacity = changedPresent(existingLaptop.batteryCapacity, command.batteryCapacity),
            storageCapacity = changedPresent(existingLaptop.storageCapacity, command.storageCapacity),
            storageSlotCount = changedPresent(existingLaptop.storageSlotCount, command.storageSlotCount),
            weight = changedPresent(existingLaptop.weight, command.weight),
            lastDetailedCrawledAt = changedPresent(existingLaptop.lastDetailedCrawledAt, command.lastDetailedCrawledAt),
            usages = changedUsages(existingLaptop.usages, command.usages),
        )
    }

    fun hasChanges(updateCommand: UpdateCrawledLaptopCommand): Boolean {
        return listOf(
            updateCommand.name,
            updateCommand.imageUrl,
            updateCommand.detailPage,
            updateCommand.productCode,
            updateCommand.price,
            updateCommand.cpuManufacturer,
            updateCommand.cpu,
            updateCommand.os,
            updateCommand.screenSize,
            updateCommand.resolution,
            updateCommand.brightness,
            updateCommand.refreshRate,
            updateCommand.ramSize,
            updateCommand.ramType,
            updateCommand.isRamReplaceable,
            updateCommand.graphicsType,
            updateCommand.tgp,
            updateCommand.thunderboltCount,
            updateCommand.usbCCount,
            updateCommand.usbACount,
            updateCommand.sdCard,
            updateCommand.isSupportsPdCharging,
            updateCommand.batteryCapacity,
            updateCommand.storageCapacity,
            updateCommand.storageSlotCount,
            updateCommand.weight,
            updateCommand.lastDetailedCrawledAt,
            updateCommand.usages,
        ).any { value -> value != null }
    }

    private fun changedText(currentValue: String?, newValue: String?): String? {
        val normalizedValue = newValue?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (currentValue?.trim() == normalizedValue) {
            return null
        }
        return normalizedValue
    }

    private fun <T : Any> changedPresent(currentValue: T?, newValue: T?): T? {
        val normalizedValue = newValue ?: return null
        if (currentValue == normalizedValue) {
            return null
        }
        return normalizedValue
    }

    private fun changedUsages(currentUsages: List<String>, newUsages: List<String>): List<String>? {
        val normalizedUsages = newUsages
            .map { usage -> usage.trim() }
            .filter { usage -> usage.isNotBlank() }
            .distinct()
        if (normalizedUsages.isEmpty() || currentUsages.sorted() == normalizedUsages.sorted()) {
            return null
        }

        return normalizedUsages
    }
}
