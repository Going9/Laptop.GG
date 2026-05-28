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
            return when (rawValue
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.lowercase()
            ) {
                null -> CORE
                CORE.storageValue -> CORE
                EXTENDED.storageValue -> EXTENDED
                NONE.storageValue, "all" -> NONE
                else -> CORE
            }
        }
    }
}
