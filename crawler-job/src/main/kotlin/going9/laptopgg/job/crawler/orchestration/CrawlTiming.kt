package going9.laptopgg.job.crawler.orchestration

import org.springframework.stereotype.Component

internal fun interface CrawlClock {
    fun currentTimeMillis(): Long
}

@Component
internal class SystemCrawlClock : CrawlClock {
    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
