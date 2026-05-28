package going9.laptopgg.job.crawler.danawa

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.job.crawler.source.CrawlSource
import going9.laptopgg.job.crawler.source.CrawlSourceResolver
import going9.laptopgg.job.crawler.source.ResolvedCrawlSources
import org.springframework.stereotype.Component

@Component
internal class DanawaCrawlSourceResolver : CrawlSourceResolver {
    override fun resolve(filterProfile: CrawlerFilterProfile): ResolvedCrawlSources {
        return ResolvedCrawlSources(
            profileName = filterProfile.storageValue,
            sources = resolveCrawlSources(filterProfile),
        )
    }

    private fun resolveCrawlSources(filterProfile: CrawlerFilterProfile): List<CrawlSource> {
        val mainSource = when (filterProfile) {
            CrawlerFilterProfile.NONE -> {
                CrawlSource(
                    key = "notebook-all",
                    listUrl = DanawaEndpoints.NOTEBOOK_LIST_URL,
                )
            }
            CrawlerFilterProfile.CORE -> {
                CrawlSource(
                    key = "notebook-core-codename",
                    listUrl = DanawaEndpoints.NOTEBOOK_LIST_URL,
                    attributeFilters = DanawaAttributeFilterCatalog.coreCpuCodenames,
                )
            }
            CrawlerFilterProfile.EXTENDED -> {
                CrawlSource(
                    key = "notebook-extended-codename",
                    listUrl = DanawaEndpoints.NOTEBOOK_LIST_URL,
                    attributeFilters = DanawaAttributeFilterCatalog.extendedCpuCodenames,
                )
            }
        }

        return listOf(
            mainSource,
            CrawlSource(
                key = "apple-macbook",
                listUrl = DanawaEndpoints.APPLE_MACBOOK_LIST_URL,
            ),
        )
    }
}
