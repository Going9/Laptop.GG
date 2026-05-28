package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.UpsertCrawledLaptopProfileCommand

interface CrawledLaptopProfilePort {
    fun upsert(command: UpsertCrawledLaptopProfileCommand): CrawledLaptopProfileState
    fun findLaptopIdsWithIncompleteStaticScores(limit: Int): List<Long>
}
