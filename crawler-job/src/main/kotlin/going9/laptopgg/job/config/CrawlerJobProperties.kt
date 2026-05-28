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
    fun validateForStartup() {
        resolvedLimit()
        resolvedStartPage()
        resolvedMaxListPages()
        resolvedDetailFetchConcurrency()
    }

    fun resolvedLimit(): Int? {
        return limit?.let { requirePositive("app.crawler.limit", it) }
    }

    fun resolvedStartPage(): Int {
        return startPage?.let { requirePositive("app.crawler.start-page", it) } ?: 1
    }

    fun resolvedFilterProfile(): CrawlerFilterProfile {
        return resolvedFilterProfileResolution().profile
    }

    fun resolvedFilterProfileResolution(): CrawlerFilterProfileResolution {
        return CrawlerFilterProfile.resolve(filterProfile)
    }

    fun resolvedMaxListPages(): Int {
        return requirePositive("app.crawler.max-list-pages", maxListPages)
    }

    fun resolvedDetailFetchConcurrency(): Int {
        return requirePositive("app.crawler.detail-fetch-concurrency", detailFetchConcurrency)
            .coerceAtMost(MAX_DETAIL_FETCH_CONCURRENCY)
    }

    private fun requirePositive(propertyName: String, value: Int): Int {
        if (value <= 0) {
            throw InvalidCrawlerJobConfigurationException("$propertyName must be positive.")
        }
        return value
    }

    companion object {
        internal const val DEFAULT_FILTER_PROFILE = "core"
        internal const val DEFAULT_MAX_LIST_PAGES = 5000
        internal const val DEFAULT_DETAIL_FETCH_CONCURRENCY = 6
        internal const val MAX_DETAIL_FETCH_CONCURRENCY = 12
    }
}

internal class InvalidCrawlerJobConfigurationException(
    message: String,
) : IllegalStateException(message)
