package going9.laptopgg.job.crawler.source

internal data class CrawlerAttributeFilter(
    val name: String,
    val value: String,
)

internal data class CrawlSource(
    val key: String,
    val listUrl: String,
    val attributeFilters: List<CrawlerAttributeFilter> = emptyList(),
)

internal enum class FilterProfile {
    NONE,
    CORE,
    EXTENDED,
}
