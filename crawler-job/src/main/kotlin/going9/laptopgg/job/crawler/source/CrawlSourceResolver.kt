package going9.laptopgg.job.crawler.source

internal interface CrawlSourceResolver {
    fun resolve(rawProfile: String?): ResolvedCrawlSources
}

internal data class ResolvedCrawlSources(
    val profileName: String,
    val sources: List<CrawlSource>,
)
