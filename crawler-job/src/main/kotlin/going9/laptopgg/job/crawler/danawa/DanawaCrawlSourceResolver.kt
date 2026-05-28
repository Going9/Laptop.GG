package going9.laptopgg.job.crawler.danawa

import going9.laptopgg.job.crawler.source.CrawlSource
import going9.laptopgg.job.crawler.source.CrawlSourceResolver
import going9.laptopgg.job.crawler.source.ResolvedCrawlSources
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class DanawaCrawlSourceResolver : CrawlSourceResolver {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun resolve(rawProfile: String?): ResolvedCrawlSources {
        val filterProfile = resolveFilterProfile(rawProfile)
        return ResolvedCrawlSources(
            profileName = filterProfile.name.lowercase(),
            sources = resolveCrawlSources(filterProfile),
        )
    }

    private fun resolveFilterProfile(rawValue: String?): DanawaFilterProfile {
        return when (rawValue?.trim()?.lowercase()) {
            null, "", "core" -> DanawaFilterProfile.CORE
            "none", "all" -> DanawaFilterProfile.NONE
            "extended" -> DanawaFilterProfile.EXTENDED
            else -> {
                logger.warn("알 수 없는 crawler filter profile='{}'. 기본값 core를 사용합니다.", rawValue)
                DanawaFilterProfile.CORE
            }
        }
    }

    private fun resolveCrawlSources(filterProfile: DanawaFilterProfile): List<CrawlSource> {
        val mainSource = when (filterProfile) {
            DanawaFilterProfile.NONE -> {
                CrawlSource(
                    key = "notebook-all",
                    listUrl = DanawaEndpoints.NOTEBOOK_LIST_URL,
                )
            }
            DanawaFilterProfile.CORE -> {
                CrawlSource(
                    key = "notebook-core-codename",
                    listUrl = DanawaEndpoints.NOTEBOOK_LIST_URL,
                    attributeFilters = DanawaAttributeFilterCatalog.coreCpuCodenames,
                )
            }
            DanawaFilterProfile.EXTENDED -> {
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

internal enum class DanawaFilterProfile {
    NONE,
    CORE,
    EXTENDED,
}
