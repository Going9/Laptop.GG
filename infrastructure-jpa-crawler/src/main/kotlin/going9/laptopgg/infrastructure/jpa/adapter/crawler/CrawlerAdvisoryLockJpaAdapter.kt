package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.run.CrawlerLockResult
import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort
import javax.sql.DataSource
import org.springframework.stereotype.Component

@Component
internal class CrawlerAdvisoryLockJpaAdapter(
    private val dataSource: DataSource,
) : CrawlerRunLockPort {
    override fun <T> withCrawlerLock(block: () -> T): CrawlerLockResult<T> {
        dataSource.connection.use { connection ->
            val acquired = connection.prepareStatement("select pg_try_advisory_lock(?)").use { statement ->
                statement.setLong(1, CRAWLER_LOCK_KEY)
                statement.executeQuery().use { resultSet ->
                    resultSet.next() && resultSet.getBoolean(1)
                }
            }

            if (!acquired) {
                return CrawlerLockResult(acquired = false, value = null)
            }

            return try {
                CrawlerLockResult(acquired = true, value = block())
            } finally {
                connection.prepareStatement("select pg_advisory_unlock(?)").use { statement ->
                    statement.setLong(1, CRAWLER_LOCK_KEY)
                    statement.executeQuery().use { }
                }
            }
        }
    }

    private companion object {
        const val CRAWLER_LOCK_KEY = 9_112_758L
    }
}
