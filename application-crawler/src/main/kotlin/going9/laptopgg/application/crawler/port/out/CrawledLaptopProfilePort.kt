package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.UpsertCrawledLaptopProfileCommand

interface CrawledLaptopProfilePort {
    fun upsert(command: UpsertCrawledLaptopProfileCommand): CrawledLaptopProfileState
    fun findLaptopIdsWithIncompleteStaticScores(limit: Int): List<Long>
}
