package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.domain.laptop.LaptopProfile

interface CrawledLaptopProfilePort {
    fun findByLaptopId(laptopId: Long): LaptopProfile?
    fun save(laptopProfile: LaptopProfile): LaptopProfile
    fun findLaptopIdsWithIncompleteStaticScores(limit: Int): List<Long>
}
