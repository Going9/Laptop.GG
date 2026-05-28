package going9.laptopgg.job.crawler.source

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile

internal interface CrawlSourceResolver {
    fun resolve(filterProfile: CrawlerFilterProfile): ResolvedCrawlSources
}

internal data class ResolvedCrawlSources(
    val profileName: String,
    val sources: List<CrawlSource>,
)
