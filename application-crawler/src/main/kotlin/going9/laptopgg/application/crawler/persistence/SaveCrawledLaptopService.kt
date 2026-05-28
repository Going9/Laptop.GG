package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort

internal class SaveCrawledLaptopService(
    private val laptopPort: CrawledLaptopPersistencePort,
    private val existingLookupLoader: ExistingCrawledLaptopLookupLoader,
    private val postSaveSynchronizer: CrawledLaptopPostSaveSynchronizer,
    private val transactionPort: CrawlerTransactionPort,
    private val changeDetector: CrawledLaptopChangeDetector = CrawledLaptopChangeDetector(),
) : SaveCrawledLaptopUseCase {
    override fun loadExistingLookup(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
        return transactionPort.read {
            existingLookupLoader.load(productCards)
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

    private fun saveListSnapshotInTransaction(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult {
        val existingLaptop = laptopPort.findWithUsageById(existingLaptopId)
            ?: throw CrawlerResourceNotFoundException("Laptop", existingLaptopId)
        val updateCommand = changeDetector.listSnapshotUpdate(existingLaptop, productCard)

        if (!changeDetector.hasChanges(updateCommand)) {
            return SaveResult.UNCHANGED
        }

        val savedLaptop = laptopPort.update(existingLaptop.id, updateCommand)
        postSaveSynchronizer.afterListSnapshot(savedLaptop, previousPrice = existingLaptop.price)
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
            return SaveResult.UNCHANGED
        }

        val savedLaptop = laptopPort.update(existingLaptop.id, updateCommand)
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
