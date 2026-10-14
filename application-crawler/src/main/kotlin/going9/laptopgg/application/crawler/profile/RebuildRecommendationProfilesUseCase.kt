package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort

fun interface RebuildRecommendationProfilesUseCase {
    fun rebuild(): Int
}

internal class RebuildRecommendationProfilesService(
    private val laptopPort: CrawledLaptopPersistencePort,
    private val profileSynchronizer: CrawledLaptopProfileSynchronizer,
    private val transactionPort: CrawlerTransactionPort,
) : RebuildRecommendationProfilesUseCase {
    override fun rebuild(): Int {
        var afterId = 0L
        var count = 0
        while (true) {
            val ids = transactionPort.read { laptopPort.findIdsAfter(afterId, 100) }
            if (ids.isEmpty()) return count
            for (id in ids) {
                // Keep each profile and its eight ranking projections atomic without locking the whole catalog.
                transactionPort.write {
                    val laptop = laptopPort.findWithUsageById(id) ?: throw CrawlerResourceNotFoundException("Laptop", id)
                    profileSynchronizer.syncProfileInTransaction(laptop)
                }
                count++
            }
            afterId = ids.last()
        }
    }
}
