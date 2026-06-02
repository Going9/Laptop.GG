package going9.laptopgg.application.crawler.run

import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort

interface CrawlerRunLockUseCase {
    fun <T> runLocked(block: () -> T): CrawlerLockResult<T>
}

internal class CrawlerRunLockService(
    private val crawlerRunLockPort: CrawlerRunLockPort,
) : CrawlerRunLockUseCase {
    override fun <T> runLocked(block: () -> T): CrawlerLockResult<T> {
        return crawlerRunLockPort.withCrawlerLock(block)
    }
}
