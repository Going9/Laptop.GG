package going9.laptopgg.application.crawler.persistence.port

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.PersistedCrawledListSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledListSnapshotCommand
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand
import java.time.Instant

interface CrawledLaptopPersistencePort {
    fun findIdsAfter(laptopId: Long, limit: Int): List<Long>
    fun recordPriceCheck(laptopId: Long, checkedAt: Instant): Boolean
    fun findListSnapshotById(laptopId: Long): PersistedCrawledListSnapshot?
    fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot?
    fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot?
    fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot?
    fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot
    fun updateListSnapshot(laptopId: Long, command: UpdateCrawledListSnapshotCommand): Boolean
    fun updateDetailSnapshot(laptopId: Long, command: UpdateCrawledLaptopCommand): Boolean
}
