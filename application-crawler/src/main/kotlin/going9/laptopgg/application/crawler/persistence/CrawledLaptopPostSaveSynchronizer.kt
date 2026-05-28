package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.profile.LaptopProfileService

class CrawledLaptopPostSaveSynchronizer(
    private val laptopProfileService: LaptopProfileService,
    private val laptopPriceHistoryService: LaptopPriceHistoryService,
) {
    internal fun afterListSnapshot(
        savedLaptop: PersistedCrawledLaptopSnapshot,
        previousPrice: Int?,
    ) {
        recordPriceHistory(savedLaptop, previousPrice)
    }

    internal fun afterDetailSnapshot(
        savedLaptop: PersistedCrawledLaptopSnapshot,
        previousPrice: Int?,
    ) {
        laptopProfileService.syncProfileInTransaction(savedLaptop)
        recordPriceHistory(savedLaptop, previousPrice)
    }

    private fun recordPriceHistory(
        savedLaptop: PersistedCrawledLaptopSnapshot,
        previousPrice: Int?,
    ) {
        laptopPriceHistoryService.recordCurrentPriceInTransaction(
            laptopId = savedLaptop.id,
            currentPrice = savedLaptop.price,
            previousPrice = previousPrice,
        )
    }
}
