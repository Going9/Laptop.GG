package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopUsage
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import org.springframework.stereotype.Component

@Component
class CrawledLaptopPersistenceJpaAdapter(
    private val laptopRepository: CrawlerLaptopRepository,
) : CrawledLaptopPersistencePort {
    override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findWithUsageById(laptopId)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findByProductCode(productCode)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findByDetailPage(detailPage)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findAllByProductCodes(productCodes: Collection<String>): List<PersistedCrawledLaptopSnapshot> {
        if (productCodes.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllByProductCodeIn(productCodes).map { laptop -> laptop.toPersistedCrawledLaptopSnapshot() }
    }

    override fun findAllByDetailPages(detailPages: Collection<String>): List<PersistedCrawledLaptopSnapshot> {
        if (detailPages.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllByDetailPageIn(detailPages).map { laptop -> laptop.toPersistedCrawledLaptopSnapshot() }
    }

    override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        return laptopRepository.save(command.toLaptop()).toPersistedCrawledLaptopSnapshot()
    }

    override fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        val laptop = laptopRepository.findWithUsageById(laptopId)
            ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        laptop.applyUpdate(command)
        return laptopRepository.save(laptop).toPersistedCrawledLaptopSnapshot()
    }

    private fun CrawledLaptopCommand.toLaptop(): Laptop {
        val laptop = Laptop(
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
            laptopUsage = mutableListOf(),
        )
        laptop.laptopUsage = usages
            .distinct()
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()
        return laptop
    }

    private fun Laptop.applyUpdate(command: UpdateCrawledLaptopCommand) {
        command.name?.let { name = it }
        command.imageUrl?.let { imageUrl = it }
        command.detailPage?.let { detailPage = it }
        command.productCode?.let { productCode = it }
        command.price?.let { price = it }
        command.cpuManufacturer?.let { cpuManufacturer = it }
        command.cpu?.let { cpu = it }
        command.os?.let { os = it }
        command.screenSize?.let { screenSize = it }
        command.resolution?.let { resolution = it }
        command.brightness?.let { brightness = it }
        command.refreshRate?.let { refreshRate = it }
        command.ramSize?.let { ramSize = it }
        command.ramType?.let { ramType = it }
        command.isRamReplaceable?.let { isRamReplaceable = it }
        command.graphicsType?.let { graphicsType = it }
        command.tgp?.let { tgp = it }
        command.thunderboltCount?.let { thunderboltCount = it }
        command.usbCCount?.let { usbCCount = it }
        command.usbACount?.let { usbACount = it }
        command.sdCard?.let { sdCard = it }
        command.isSupportsPdCharging?.let { isSupportsPdCharging = it }
        command.batteryCapacity?.let { batteryCapacity = it }
        command.storageCapacity?.let { storageCapacity = it }
        command.storageSlotCount?.let { storageSlotCount = it }
        command.weight?.let { weight = it }
        command.lastDetailedCrawledAt?.let { lastDetailedCrawledAt = it }
        command.usages?.let { usages ->
            laptopUsage.clear()
            usages.forEach { usage ->
                laptopUsage.add(LaptopUsage(usage = usage, laptop = this))
            }
        }
    }
}
