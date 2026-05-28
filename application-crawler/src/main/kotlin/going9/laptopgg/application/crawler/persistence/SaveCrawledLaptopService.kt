package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort

internal class SaveCrawledLaptopService(
    private val laptopPort: CrawledLaptopPersistencePort,
    private val postSaveSynchronizer: CrawledLaptopPostSaveSynchronizer,
    private val transactionPort: CrawlerTransactionPort,
    private val changeDetector: CrawledLaptopChangeDetector = CrawledLaptopChangeDetector(),
    private val validator: CrawledLaptopCommandValidator = CrawledLaptopCommandValidator(),
) : SaveCrawledLaptopUseCase {
    override fun saveListSnapshot(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult {
        validator.validateExistingLaptopId(existingLaptopId)
        val normalizedProductCard = changeDetector.normalizedProductCard(productCard)
        validator.validateProductCard(normalizedProductCard)
        return transactionPort.write {
            saveListSnapshotInTransaction(existingLaptopId, normalizedProductCard)
        }
    }

    override fun saveOrUpdateLaptop(command: CrawledLaptopCommand, existingLaptopId: Long?): SaveResult {
        existingLaptopId?.let(validator::validateExistingLaptopId)
        val normalizedCommand = changeDetector.normalizedDetailCommand(command)
        validator.validateLaptopCommand(normalizedCommand)
        return transactionPort.write {
            saveOrUpdateLaptopInTransaction(normalizedCommand, existingLaptopId)
        }
    }

    private fun saveListSnapshotInTransaction(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult {
        val existingLaptop = laptopPort.findListSnapshotById(existingLaptopId)
            ?: throw CrawlerResourceNotFoundException("Laptop", existingLaptopId)
        val updateCommand = changeDetector.listSnapshotUpdate(existingLaptop, productCard)

        if (!changeDetector.hasChanges(updateCommand)) {
            return SaveResult.UNCHANGED
        }

        if (!laptopPort.updateListSnapshot(existingLaptop.id, updateCommand)) {
            throw CrawlerResourceNotFoundException("Laptop", existingLaptop.id)
        }
        postSaveSynchronizer.afterListSnapshot(
            laptopId = existingLaptop.id,
            currentPrice = updateCommand.price ?: existingLaptop.price,
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
            postSaveSynchronizer.afterDetailSnapshot(savedLaptop, previousPrice = null)
            return SaveResult.CREATED
        }

        val updateCommand = changeDetector.detailUpdate(existingLaptop, command)
        if (!changeDetector.hasChanges(updateCommand)) {
            postSaveSynchronizer.afterDetailSnapshot(existingLaptop, previousPrice = existingLaptop.price)
            return SaveResult.UNCHANGED
        }

        if (!laptopPort.updateDetailSnapshot(existingLaptop.id, updateCommand)) {
            throw CrawlerResourceNotFoundException("Laptop", existingLaptop.id)
        }
        val savedLaptop = changeDetector.applyDetailUpdate(existingLaptop, updateCommand)
        postSaveSynchronizer.afterDetailSnapshot(savedLaptop, previousPrice = existingLaptop.price)
        return SaveResult.UPDATED
    }

    private fun findExistingLaptop(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot? {
        command.productCode?.let { productCode ->
            laptopPort.findByProductCode(productCode)?.let { return it }
        }

        laptopPort.findByDetailPage(command.detailPage)?.let { return it }

        return null
    }
}
