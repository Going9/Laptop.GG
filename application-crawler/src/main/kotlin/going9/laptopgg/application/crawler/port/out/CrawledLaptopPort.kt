package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.domain.laptop.Laptop

interface CrawledLaptopPort {
    fun findWithUsageById(laptopId: Long): Laptop?
    fun findAllWithUsageByIds(laptopIds: Collection<Long>): List<Laptop>
    fun findIdsWithoutProfile(limit: Int): List<Long>
    fun findByProductCode(productCode: String): Laptop?
    fun findByDetailPage(detailPage: String): Laptop?
    fun findAllByProductCodes(productCodes: Collection<String>): List<Laptop>
    fun findAllByDetailPages(detailPages: Collection<String>): List<Laptop>
    fun findAllByDetailPageContaining(detailPageToken: String): List<Laptop>
    fun save(laptop: Laptop): Laptop
}
