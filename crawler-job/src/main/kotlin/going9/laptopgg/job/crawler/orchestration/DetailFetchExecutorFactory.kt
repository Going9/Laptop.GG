package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.config.CrawlerJobProperties
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import org.springframework.stereotype.Component

internal fun interface DetailFetchExecutorFactory {
    fun create(): DetailFetchExecutor
}

@Component
internal class ConfiguredDetailFetchExecutorFactory(
    private val crawlerJobProperties: CrawlerJobProperties,
) : DetailFetchExecutorFactory {
    override fun create(): DetailFetchExecutor {
        return DetailFetchExecutor.fixed(crawlerJobProperties.resolvedDetailFetchConcurrency())
    }
}
