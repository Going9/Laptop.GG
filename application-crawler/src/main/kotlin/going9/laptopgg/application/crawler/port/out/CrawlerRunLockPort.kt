package going9.laptopgg.application.crawler.port.out

interface CrawlerRunLockPort {
    fun <T> withCrawlerLock(block: () -> T): CrawlerLockResult<T>
}

data class CrawlerLockResult<T>(
    val acquired: Boolean,
    val value: T?,
)
