package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.CrawlerLockResult

interface CrawlerRunLockPort {
    fun <T> withCrawlerLock(block: () -> T): CrawlerLockResult<T>
}
