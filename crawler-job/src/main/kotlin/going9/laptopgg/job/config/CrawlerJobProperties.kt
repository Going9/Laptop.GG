package going9.laptopgg.job.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.crawler")
data class CrawlerJobProperties(
    val runOnStartup: Boolean = false,
    val limit: Int? = null,
    val startPage: Int? = null,
    val filterProfile: String = DEFAULT_FILTER_PROFILE,
) {
    fun resolvedLimit(): Int? {
        return limit?.takeIf { it > 0 }
    }

    fun resolvedStartPage(): Int {
        return startPage?.takeIf { it > 0 } ?: 1
    }

    fun resolvedFilterProfile(): String {
        return normalizedFilterProfile(filterProfile)
    }

    companion object {
        internal const val DEFAULT_FILTER_PROFILE = "core"

        internal fun normalizedFilterProfile(rawValue: String?): String {
            return rawValue
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_FILTER_PROFILE
        }
    }
}
