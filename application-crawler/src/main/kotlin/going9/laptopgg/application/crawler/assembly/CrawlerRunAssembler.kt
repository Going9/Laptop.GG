package going9.laptopgg.application.crawler.assembly

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.run.CrawlerRunLockService
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.TrackCrawlerRunService
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort

object CrawlerRunAssembler {
    fun createTrackCrawlerRunUseCase(
        crawlerRunPort: CrawlerRunPort,
        transactionPort: CrawlerTransactionPort,
    ): TrackCrawlerRunUseCase {
        return TrackCrawlerRunService(
            crawlerRunPort = crawlerRunPort,
            transactionPort = transactionPort,
        )
    }

    fun createCrawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunLockService(crawlerRunLockPort)
    }
}
