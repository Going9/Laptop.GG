package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.price.LaptopPriceHistoryRecorder
import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileSynchronizer

internal class CrawledLaptopPostSaveSynchronizer(
    private val laptopProfileSynchronizer: CrawledLaptopProfileSynchronizer,
    private val laptopPriceHistoryRecorder: LaptopPriceHistoryRecorder,
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
        laptopProfileSynchronizer.syncProfileInTransaction(savedLaptop)
        recordPriceHistory(savedLaptop, previousPrice)
    }

    private fun recordPriceHistory(
        savedLaptop: PersistedCrawledLaptopSnapshot,
        previousPrice: Int?,
    ) {
        laptopPriceHistoryRecorder.recordCurrentPriceInTransaction(
            laptopId = savedLaptop.id,
            currentPrice = savedLaptop.price,
            previousPrice = previousPrice,
        )
    }
}
