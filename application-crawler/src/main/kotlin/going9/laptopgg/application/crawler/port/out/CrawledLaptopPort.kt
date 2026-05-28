package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand

interface CrawledLaptopPort {
    fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot?
    fun findAllWithUsageByIds(laptopIds: Collection<Long>): List<PersistedCrawledLaptopSnapshot>
    fun findIdsWithoutProfile(limit: Int): List<Long>
    fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot?
    fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot?
    fun findAllByProductCodes(productCodes: Collection<String>): List<PersistedCrawledLaptopSnapshot>
    fun findAllByDetailPages(detailPages: Collection<String>): List<PersistedCrawledLaptopSnapshot>
    fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot
    fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot
}
