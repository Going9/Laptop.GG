package going9.laptopgg.job.crawler.source

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

internal object CrawlerUrls {
    const val NOTEBOOK_LIST_URL = "https://prod.danawa.com/list/?cate=112758"
    const val APPLE_MACBOOK_LIST_URL = "https://prod.danawa.com/list/?cate=11236463"
}
