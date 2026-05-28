package going9.laptopgg.application.crawler.profile.port

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot

interface CrawledLaptopProfileSourcePort {
    fun findAllWithUsageByIds(laptopIds: Collection<Long>): List<PersistedCrawledLaptopSnapshot>
    fun findIdsWithoutProfile(limit: Int): List<Long>
}
