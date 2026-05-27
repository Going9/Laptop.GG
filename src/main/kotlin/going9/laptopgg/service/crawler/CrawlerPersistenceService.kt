package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopPriceHistoryService
import going9.laptopgg.service.LaptopProfileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CrawlerPersistenceService(
    private val laptopRepository: LaptopRepository,
    private val laptopProfileService: LaptopProfileService,
    private val laptopPriceHistoryService: LaptopPriceHistoryService,
) {
    internal data class ExistingLookup(
        val byProductCode: Map<String, Laptop>,
        val byDetailPage: Map<String, Laptop>,
    )

    @Transactional(readOnly = true)
    internal fun loadExistingLookup(productCards: List<ProductCard>): ExistingLookup {
        if (productCards.isEmpty()) {
            return ExistingLookup(emptyMap(), emptyMap())
        }

        val byProductCode = laptopRepository.findAllByProductCodeIn(productCards.map { it.productCode }.distinct())
            .mapNotNull { laptop -> laptop.productCode?.let { it to laptop } }
            .toMap()
        val byDetailPage = laptopRepository.findAllByDetailPageIn(productCards.map { it.detailPage }.distinct())
            .associateBy { laptop -> laptop.detailPage }

        return ExistingLookup(
            byProductCode = byProductCode,
            byDetailPage = byDetailPage,
        )
    }

    internal fun findExistingLaptop(
        productCard: ProductCard,
        existingLookup: ExistingLookup,
    ): Laptop? {
        return existingLookup.byProductCode[productCard.productCode]
            ?: existingLookup.byDetailPage[productCard.detailPage]
    }

    @Transactional
    internal fun saveOrUpdateLaptop(laptop: Laptop): SaveResult {
        return saveOrUpdateResolvedLaptop(
            laptop = laptop,
            existingLaptop = findExistingLaptop(laptop, allowLegacyFallback = true),
        )
    }

    @Transactional
    internal fun saveOrUpdateLaptop(
        laptop: Laptop,
        existingLaptopHint: Laptop?,
    ): SaveResult {
        return saveOrUpdateResolvedLaptop(
            laptop = laptop,
            existingLaptop = existingLaptopHint,
        )
    }

    @Transactional
    internal fun saveListSnapshot(
        existingLaptop: Laptop,
        productCard: ProductCard,
    ): SaveResult {
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

        val savedLaptop = laptopRepository.save(existingLaptop)
        laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice)
        return SaveResult.UPDATED
    }

    private fun saveOrUpdateResolvedLaptop(
        laptop: Laptop,
        existingLaptop: Laptop?,
    ): SaveResult {
        if (existingLaptop == null) {
            val savedLaptop = laptopRepository.save(laptop)
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
            val savedLaptop = laptopRepository.save(existingLaptop)
            laptopProfileService.syncProfile(savedLaptop)
            laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice)
            SaveResult.UPDATED
        } else {
            SaveResult.UNCHANGED
        }
    }

    private fun findExistingLaptop(laptop: Laptop, allowLegacyFallback: Boolean): Laptop? {
        laptop.productCode?.let { productCode ->
            laptopRepository.findByProductCode(productCode)?.let { return it }
        }

        laptopRepository.findByDetailPage(laptop.detailPage)?.let { return it }

        if (allowLegacyFallback) {
            laptop.productCode?.let { productCode ->
                laptopRepository.findAllByDetailPageContaining("pcode=$productCode")
                    .singleOrNull()
                    ?.let { return it }
            }
        }

        return null
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
