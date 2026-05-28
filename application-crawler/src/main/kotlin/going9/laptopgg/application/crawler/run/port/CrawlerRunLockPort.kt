package going9.laptopgg.application.crawler.run.port

import going9.laptopgg.application.crawler.run.CrawlerLockResult

interface CrawlerRunLockPort {
    fun <T> withCrawlerLock(block: () -> T): CrawlerLockResult<T>
}
