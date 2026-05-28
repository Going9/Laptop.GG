package going9.laptopgg.application.crawler.persistence.port

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand

interface CrawledLaptopPersistencePort {
    fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot?
    fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot?
    fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot?
    fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot>
    fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot>
    fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot
    fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot
}
