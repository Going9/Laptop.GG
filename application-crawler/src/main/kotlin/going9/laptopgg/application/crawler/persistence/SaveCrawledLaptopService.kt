package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.port.out.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.port.out.CrawlerTransactionPort

class SaveCrawledLaptopService(
    private val laptopPort: CrawledLaptopPersistencePort,
    private val laptopProfileService: LaptopProfileService,
    private val laptopPriceHistoryService: LaptopPriceHistoryService,
    private val transactionPort: CrawlerTransactionPort,
) : SaveCrawledLaptopUseCase {
    override fun loadExistingLookup(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
        return transactionPort.read {
            loadExistingLookupInTransaction(productCards)
        }
    }

    override fun saveListSnapshot(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult {
        return transactionPort.write {
            saveListSnapshotInTransaction(existingLaptopId, productCard)
        }
    }

    override fun saveOrUpdateLaptop(command: CrawledLaptopCommand, existingLaptopId: Long?): SaveResult {
        return transactionPort.write {
            saveOrUpdateLaptopInTransaction(command, existingLaptopId)
        }
    }

    private fun loadExistingLookupInTransaction(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
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

    private fun saveListSnapshotInTransaction(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult {
        val existingLaptop = laptopPort.findWithUsageById(existingLaptopId)
            ?: throw IllegalArgumentException("Laptop not found: $existingLaptopId")
        val updateCommand = UpdateCrawledLaptopCommand(
            name = changedText(existingLaptop.name, productCard.productName),
            imageUrl = changedText(existingLaptop.imageUrl, productCard.imageUrl),
            detailPage = changedText(existingLaptop.detailPage, productCard.detailPage),
            productCode = changedText(existingLaptop.productCode, productCard.productCode),
            price = changedPresent(existingLaptop.price, productCard.price),
        )

        if (!updateCommand.hasChanges()) {
            return SaveResult.UNCHANGED
        }

        val savedLaptop = laptopPort.update(existingLaptop.id, updateCommand)
        laptopPriceHistoryService.recordCurrentPrice(
            laptopId = savedLaptop.id,
            currentPrice = savedLaptop.price,
            previousPrice = existingLaptop.price,
        )
        return SaveResult.UPDATED
    }

    private fun saveOrUpdateLaptopInTransaction(command: CrawledLaptopCommand, existingLaptopId: Long?): SaveResult {
        val existingLaptop = existingLaptopId?.let(laptopPort::findWithUsageById)
            ?: findExistingLaptop(command)

        return saveOrUpdateResolvedLaptop(command, existingLaptop)
    }

    private fun saveOrUpdateResolvedLaptop(
        command: CrawledLaptopCommand,
        existingLaptop: PersistedCrawledLaptopSnapshot?,
    ): SaveResult {
        if (existingLaptop == null) {
            val savedLaptop = laptopPort.create(command)
            laptopProfileService.syncProfile(savedLaptop)
            laptopPriceHistoryService.recordCurrentPrice(
                laptopId = savedLaptop.id,
                currentPrice = savedLaptop.price,
                previousPrice = null,
            )
            return SaveResult.CREATED
        }

        val updateCommand = existingLaptop.toUpdateCommand(command)
        if (!updateCommand.hasChanges()) {
            return SaveResult.UNCHANGED
        }

        val savedLaptop = laptopPort.update(existingLaptop.id, updateCommand)
        laptopProfileService.syncProfile(savedLaptop)
        laptopPriceHistoryService.recordCurrentPrice(
            laptopId = savedLaptop.id,
            currentPrice = savedLaptop.price,
            previousPrice = existingLaptop.price,
        )
        return SaveResult.UPDATED
    }

    private fun findExistingLaptop(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot? {
        command.productCode?.let { productCode ->
            laptopPort.findByProductCode(productCode)?.let { return it }
        }

        laptopPort.findByDetailPage(command.detailPage)?.let { return it }

        return null
    }

    private fun PersistedCrawledLaptopSnapshot.toUpdateCommand(command: CrawledLaptopCommand): UpdateCrawledLaptopCommand {
        return UpdateCrawledLaptopCommand(
            name = changedText(name, command.name),
            imageUrl = changedText(imageUrl, command.imageUrl),
            detailPage = changedText(detailPage, command.detailPage),
            productCode = changedText(productCode, command.productCode),
            price = changedPresent(price, command.price),
            cpuManufacturer = changedText(cpuManufacturer, command.cpuManufacturer),
            cpu = changedText(cpu, command.cpu),
            os = changedText(os, command.os),
            screenSize = changedPresent(screenSize, command.screenSize),
            resolution = changedText(resolution, command.resolution),
            brightness = changedPresent(brightness, command.brightness),
            refreshRate = changedPresent(refreshRate, command.refreshRate),
            ramSize = changedPresent(ramSize, command.ramSize),
            ramType = changedText(ramType, command.ramType),
            isRamReplaceable = changedPresent(isRamReplaceable, command.isRamReplaceable),
            graphicsType = changedText(graphicsType, command.graphicsType),
            tgp = changedPresent(tgp, command.tgp),
            thunderboltCount = changedPresent(thunderboltCount, command.thunderboltCount),
            usbCCount = changedPresent(usbCCount, command.usbCCount),
            usbACount = changedPresent(usbACount, command.usbACount),
            sdCard = changedText(sdCard, command.sdCard),
            isSupportsPdCharging = changedPresent(isSupportsPdCharging, command.isSupportsPdCharging),
            batteryCapacity = changedPresent(batteryCapacity, command.batteryCapacity),
            storageCapacity = changedPresent(storageCapacity, command.storageCapacity),
            storageSlotCount = changedPresent(storageSlotCount, command.storageSlotCount),
            weight = changedPresent(weight, command.weight),
            lastDetailedCrawledAt = changedPresent(lastDetailedCrawledAt, command.lastDetailedCrawledAt),
            usages = changedUsages(usages, command.usages),
        )
    }

    private fun PersistedCrawledLaptopSnapshot.toExistingSnapshot(): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = id,
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
            usageCount = usages.size,
        )
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

    private fun UpdateCrawledLaptopCommand.hasChanges(): Boolean {
        return listOf(
            name,
            imageUrl,
            detailPage,
            productCode,
            price,
            cpuManufacturer,
            cpu,
            os,
            screenSize,
            resolution,
            brightness,
            refreshRate,
            ramSize,
            ramType,
            isRamReplaceable,
            graphicsType,
            tgp,
            thunderboltCount,
            usbCCount,
            usbACount,
            sdCard,
            isSupportsPdCharging,
            batteryCapacity,
            storageCapacity,
            storageSlotCount,
            weight,
            lastDetailedCrawledAt,
            usages,
        ).any { value -> value != null }
    }
}
