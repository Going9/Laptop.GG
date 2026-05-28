package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawlerRunLockPort

interface CrawlerRunLockUseCase {
    fun <T> runLocked(block: () -> T): CrawlerLockResult<T>
}

class CrawlerRunLockService(
    private val crawlerRunLockPort: CrawlerRunLockPort,
) : CrawlerRunLockUseCase {
    override fun <T> runLocked(block: () -> T): CrawlerLockResult<T> {
        return crawlerRunLockPort.withCrawlerLock(block)
    }
}

data class CrawlerLockResult<T>(
    val acquired: Boolean,
    val value: T?,
)
