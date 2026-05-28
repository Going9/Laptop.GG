package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopUsage

internal object CrawledLaptopEntityMapper {
    fun newLaptop(command: CrawledLaptopCommand): Laptop {
        val laptop = Laptop(
            name = command.name,
            imageUrl = command.imageUrl,
            detailPage = command.detailPage,
            productCode = command.productCode,
            price = command.price,
            cpuManufacturer = command.cpuManufacturer,
            cpu = command.cpu,
            os = command.os,
            screenSize = command.screenSize,
            resolution = command.resolution,
            brightness = command.brightness,
            refreshRate = command.refreshRate,
            ramSize = command.ramSize,
            ramType = command.ramType,
            isRamReplaceable = command.isRamReplaceable,
            graphicsType = command.graphicsType,
            tgp = command.tgp,
            thunderboltCount = command.thunderboltCount,
            usbCCount = command.usbCCount,
            usbACount = command.usbACount,
            sdCard = command.sdCard,
            isSupportsPdCharging = command.isSupportsPdCharging,
            batteryCapacity = command.batteryCapacity,
            storageCapacity = command.storageCapacity,
            storageSlotCount = command.storageSlotCount,
            weight = command.weight,
            lastDetailedCrawledAt = command.lastDetailedCrawledAt,
            laptopUsage = mutableListOf(),
        )
        replaceUsages(laptop, command.usages)
        return laptop
    }

    fun applyUpdate(laptop: Laptop, command: UpdateCrawledLaptopCommand) {
        command.name?.let { laptop.name = it }
        command.imageUrl?.let { laptop.imageUrl = it }
        command.detailPage?.let { laptop.detailPage = it }
        command.productCode?.let { laptop.productCode = it }
        command.price?.let { laptop.price = it }
        command.cpuManufacturer?.let { laptop.cpuManufacturer = it }
        command.cpu?.let { laptop.cpu = it }
        command.os?.let { laptop.os = it }
        command.screenSize?.let { laptop.screenSize = it }
        command.resolution?.let { laptop.resolution = it }
        command.brightness?.let { laptop.brightness = it }
        command.refreshRate?.let { laptop.refreshRate = it }
        command.ramSize?.let { laptop.ramSize = it }
        command.ramType?.let { laptop.ramType = it }
        command.isRamReplaceable?.let { laptop.isRamReplaceable = it }
        command.graphicsType?.let { laptop.graphicsType = it }
        command.tgp?.let { laptop.tgp = it }
        command.thunderboltCount?.let { laptop.thunderboltCount = it }
        command.usbCCount?.let { laptop.usbCCount = it }
        command.usbACount?.let { laptop.usbACount = it }
        command.sdCard?.let { laptop.sdCard = it }
        command.isSupportsPdCharging?.let { laptop.isSupportsPdCharging = it }
        command.batteryCapacity?.let { laptop.batteryCapacity = it }
        command.storageCapacity?.let { laptop.storageCapacity = it }
        command.storageSlotCount?.let { laptop.storageSlotCount = it }
        command.weight?.let { laptop.weight = it }
        command.lastDetailedCrawledAt?.let { laptop.lastDetailedCrawledAt = it }
        command.usages?.let { usages -> replaceUsages(laptop, usages) }
    }

    private fun replaceUsages(laptop: Laptop, usages: List<String>) {
        laptop.laptopUsage.clear()
        laptop.laptopUsage.addAll(
            usages
                .distinct()
                .map { usage -> LaptopUsage(usage = usage, laptop = laptop) },
        )
    }
}
