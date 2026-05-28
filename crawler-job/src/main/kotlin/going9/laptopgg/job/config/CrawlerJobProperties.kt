package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.application.crawler.run.CrawlerFilterProfileResolution
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.crawler")
internal data class CrawlerJobProperties(
    val runOnStartup: Boolean = false,
    val limit: Int? = null,
    val startPage: Int? = null,
    val filterProfile: String = DEFAULT_FILTER_PROFILE,
    val maxListPages: Int = DEFAULT_MAX_LIST_PAGES,
    val detailFetchConcurrency: Int = DEFAULT_DETAIL_FETCH_CONCURRENCY,
) {
    fun resolvedLimit(): Int? {
        return limit?.takeIf { it > 0 }
    }

    fun resolvedStartPage(): Int {
        return startPage?.takeIf { it > 0 } ?: 1
    }

    fun resolvedFilterProfile(): CrawlerFilterProfile {
        return resolvedFilterProfileResolution().profile
    }

    fun resolvedFilterProfileResolution(): CrawlerFilterProfileResolution {
        return CrawlerFilterProfile.resolve(filterProfile)
    }

    fun resolvedMaxListPages(): Int {
        return maxListPages.takeIf { it > 0 } ?: DEFAULT_MAX_LIST_PAGES
    }

    fun resolvedDetailFetchConcurrency(): Int {
        return (detailFetchConcurrency.takeIf { it > 0 } ?: DEFAULT_DETAIL_FETCH_CONCURRENCY)
            .coerceAtMost(MAX_DETAIL_FETCH_CONCURRENCY)
    }

    companion object {
        internal const val DEFAULT_FILTER_PROFILE = "core"
        internal const val DEFAULT_MAX_LIST_PAGES = 5000
        internal const val DEFAULT_DETAIL_FETCH_CONCURRENCY = 6
        internal const val MAX_DETAIL_FETCH_CONCURRENCY = 12
    }
}
