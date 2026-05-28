package going9.laptopgg.application.crawler

import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SaveCrawledLaptopService(
    private val laptopPort: LaptopPort,
    private val laptopProfileService: LaptopProfileService,
    private val laptopPriceHistoryService: LaptopPriceHistoryService,
) : SaveCrawledLaptopUseCase {
    @Transactional(readOnly = true)
    override fun loadExistingLookup(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
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

    @Transactional
    override fun saveListSnapshot(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult {
        val existingLaptop = laptopPort.findWithUsageById(existingLaptopId)
            ?: throw IllegalArgumentException("Laptop not found: $existingLaptopId")
        val previousPrice = existingLaptop.price
        var changed = false

        changed = updateTextField(existingLaptop.name, productCard.productName) { existingLaptop.name = it } || changed
        changed = updateTextField(existingLaptop.imageUrl, productCard.imageUrl) { existingLaptop.imageUrl = it } || changed
        changed = updateTextField(existingLaptop.detailPage, productCard.detailPage) { existingLaptop.detailPage = it } || changed
        changed = updateTextField(existingLaptop.productCode, productCard.productCode) { existingLaptop.productCode = it } || changed
        changed = updatePresentField(existingLaptop.price, productCard.price) { existingLaptop.price = it } || changed

        if (!changed) {
            return SaveResult.UNCHANGED
        }

        val savedLaptop = laptopPort.save(existingLaptop)
        laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice)
        return SaveResult.UPDATED
    }

    @Transactional
    override fun saveOrUpdateLaptop(command: CrawledLaptopCommand, existingLaptopId: Long?): SaveResult {
        val crawledLaptop = command.toLaptop()
        val existingLaptop = existingLaptopId?.let(laptopPort::findWithUsageById)
            ?: findExistingLaptop(crawledLaptop, allowLegacyFallback = existingLaptopId == null)

        return saveOrUpdateResolvedLaptop(crawledLaptop, existingLaptop)
    }

    private fun saveOrUpdateResolvedLaptop(
        laptop: Laptop,
        existingLaptop: Laptop?,
    ): SaveResult {
        if (existingLaptop == null) {
            val savedLaptop = laptopPort.save(laptop)
            laptopProfileService.syncProfile(savedLaptop)
            laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice = null)
            return SaveResult.CREATED
        }

        val previousPrice = existingLaptop.price
        var changed = false

        changed = updateTextField(existingLaptop.name, laptop.name) { existingLaptop.name = it } || changed
        changed = updateTextField(existingLaptop.imageUrl, laptop.imageUrl) { existingLaptop.imageUrl = it } || changed
        changed = updateTextField(existingLaptop.detailPage, laptop.detailPage) { existingLaptop.detailPage = it } || changed
        changed = updateTextField(existingLaptop.productCode, laptop.productCode) { existingLaptop.productCode = it } || changed
        changed = updatePresentField(existingLaptop.price, laptop.price) { existingLaptop.price = it } || changed
        changed = updateTextField(existingLaptop.cpuManufacturer, laptop.cpuManufacturer) { existingLaptop.cpuManufacturer = it } || changed
        changed = updateTextField(existingLaptop.cpu, laptop.cpu) { existingLaptop.cpu = it } || changed
        changed = updateTextField(existingLaptop.os, laptop.os) { existingLaptop.os = it } || changed
        changed = updatePresentField(existingLaptop.screenSize, laptop.screenSize) { existingLaptop.screenSize = it } || changed
        changed = updateTextField(existingLaptop.resolution, laptop.resolution) { existingLaptop.resolution = it } || changed
        changed = updatePresentField(existingLaptop.brightness, laptop.brightness) { existingLaptop.brightness = it } || changed
        changed = updatePresentField(existingLaptop.refreshRate, laptop.refreshRate) { existingLaptop.refreshRate = it } || changed
        changed = updatePresentField(existingLaptop.ramSize, laptop.ramSize) { existingLaptop.ramSize = it } || changed
        changed = updateTextField(existingLaptop.ramType, laptop.ramType) { existingLaptop.ramType = it } || changed
        changed = updatePresentField(existingLaptop.isRamReplaceable, laptop.isRamReplaceable) { existingLaptop.isRamReplaceable = it } || changed
        changed = updateTextField(existingLaptop.graphicsType, laptop.graphicsType) { existingLaptop.graphicsType = it } || changed
        changed = updatePresentField(existingLaptop.tgp, laptop.tgp) { existingLaptop.tgp = it } || changed
        changed = updatePresentField(existingLaptop.thunderboltCount, laptop.thunderboltCount) { existingLaptop.thunderboltCount = it } || changed
        changed = updatePresentField(existingLaptop.usbCCount, laptop.usbCCount) { existingLaptop.usbCCount = it } || changed
        changed = updatePresentField(existingLaptop.usbACount, laptop.usbACount) { existingLaptop.usbACount = it } || changed
        changed = updateTextField(existingLaptop.sdCard, laptop.sdCard) { existingLaptop.sdCard = it } || changed
        changed = updatePresentField(existingLaptop.isSupportsPdCharging, laptop.isSupportsPdCharging) { existingLaptop.isSupportsPdCharging = it } || changed
        changed = updatePresentField(existingLaptop.batteryCapacity, laptop.batteryCapacity) { existingLaptop.batteryCapacity = it } || changed
        changed = updatePresentField(existingLaptop.storageCapacity, laptop.storageCapacity) { existingLaptop.storageCapacity = it } || changed
        changed = updatePresentField(existingLaptop.storageSlotCount, laptop.storageSlotCount) { existingLaptop.storageSlotCount = it } || changed
        changed = updatePresentField(existingLaptop.weight, laptop.weight) { existingLaptop.weight = it } || changed
        changed = updatePresentField(existingLaptop.lastDetailedCrawledAt, laptop.lastDetailedCrawledAt) { existingLaptop.lastDetailedCrawledAt = it } || changed

        val existingUsages = existingLaptop.laptopUsage.map { it.usage }.sorted()
        val newUsages = laptop.laptopUsage.map { it.usage }.sorted()
        if (newUsages.isNotEmpty() && existingUsages != newUsages) {
            existingLaptop.laptopUsage.clear()
            laptop.laptopUsage.forEach { usage ->
                existingLaptop.laptopUsage.add(LaptopUsage(usage = usage.usage, laptop = existingLaptop))
            }
            changed = true
        }

        return if (changed) {
            val savedLaptop = laptopPort.save(existingLaptop)
            laptopProfileService.syncProfile(savedLaptop)
            laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice)
            SaveResult.UPDATED
        } else {
            SaveResult.UNCHANGED
        }
    }

    private fun findExistingLaptop(laptop: Laptop, allowLegacyFallback: Boolean): Laptop? {
        laptop.productCode?.let { productCode ->
            laptopPort.findByProductCode(productCode)?.let { return it }
        }

        laptopPort.findByDetailPage(laptop.detailPage)?.let { return it }

        if (allowLegacyFallback) {
            laptop.productCode?.let { productCode ->
                laptopPort.findAllByDetailPageContaining("pcode=$productCode")
                    .singleOrNull()
                    ?.let { return it }
            }
        }

        return null
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

    private fun Laptop.toExistingSnapshot(): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = requireNotNull(id) { "Persisted laptop id must not be null." },
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
            usageCount = laptopUsage.size,
        )
    }

    private fun updateTextField(currentValue: String?, newValue: String?, updater: (String) -> Unit): Boolean {
        val normalizedValue = newValue?.trim()?.takeIf { it.isNotBlank() } ?: return false
        if (currentValue?.trim() == normalizedValue) {
            return false
        }
        updater(normalizedValue)
        return true
    }

    private fun <T : Any> updatePresentField(currentValue: T?, newValue: T?, updater: (T) -> Unit): Boolean {
        val normalizedValue = newValue ?: return false
        if (currentValue == normalizedValue) {
            return false
        }
        updater(normalizedValue)
        return true
    }
}
