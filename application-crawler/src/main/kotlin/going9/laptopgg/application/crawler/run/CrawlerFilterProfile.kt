package going9.laptopgg.application.crawler.run

enum class CrawlerFilterProfile(
    val storageValue: String,
) {
    CORE("core"),
    EXTENDED("extended"),
    NONE("none"),
    ;

    companion object {
        fun from(rawValue: String?): CrawlerFilterProfile {
            return resolve(rawValue).profile
        }

        fun resolve(rawValue: String?): CrawlerFilterProfileResolution {
            val normalizedValue = rawValue
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.lowercase()
            return when (normalizedValue) {
                null -> CrawlerFilterProfileResolution(profile = CORE, usedDefaultForUnknownValue = false)
                CORE.storageValue -> CrawlerFilterProfileResolution(profile = CORE, usedDefaultForUnknownValue = false)
                EXTENDED.storageValue -> CrawlerFilterProfileResolution(profile = EXTENDED, usedDefaultForUnknownValue = false)
                NONE.storageValue, "all" -> CrawlerFilterProfileResolution(profile = NONE, usedDefaultForUnknownValue = false)
                else -> CrawlerFilterProfileResolution(
                    profile = CORE,
                    usedDefaultForUnknownValue = true,
                    rawValue = normalizedValue,
                )
            }
        }
    }
}

data class CrawlerFilterProfileResolution(
    val profile: CrawlerFilterProfile,
    val usedDefaultForUnknownValue: Boolean,
    val rawValue: String? = null,
)
