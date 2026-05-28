package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
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

    fun newLaptopUsages(laptop: Laptop, usages: List<String>): List<LaptopUsage> {
        return usages
            .distinct()
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
    }

    private fun replaceUsages(laptop: Laptop, usages: List<String>) {
        laptop.laptopUsage.clear()
        laptop.laptopUsage.addAll(newLaptopUsages(laptop, usages))
    }
}
