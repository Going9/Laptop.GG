package going9.laptopgg.job.crawler.source

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlSourceResolver {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun resolveFilterProfile(rawValue: String?): FilterProfile {
        return when (rawValue?.trim()?.lowercase()) {
            null, "", "core" -> FilterProfile.CORE
            "none", "all" -> FilterProfile.NONE
            "extended" -> FilterProfile.EXTENDED
            else -> {
                logger.warn("알 수 없는 crawler filter profile='{}'. 기본값 core를 사용합니다.", rawValue)
                FilterProfile.CORE
            }
        }
    }

    internal fun resolveCrawlSources(filterProfile: FilterProfile): List<CrawlSource> {
        val mainSource = when (filterProfile) {
            FilterProfile.NONE -> {
                CrawlSource(
                    key = "notebook-all",
                    listUrl = CrawlerUrls.NOTEBOOK_LIST_URL,
                )
            }
            FilterProfile.CORE -> {
                CrawlSource(
                    key = "notebook-core-codename",
                    listUrl = CrawlerUrls.NOTEBOOK_LIST_URL,
                    attributeFilters = CrawlerFilterSets.coreCpuCodenames,
                )
            }
            FilterProfile.EXTENDED -> {
                CrawlSource(
                    key = "notebook-extended-codename",
                    listUrl = CrawlerUrls.NOTEBOOK_LIST_URL,
                    attributeFilters = CrawlerFilterSets.extendedCpuCodenames,
                )
            }
        }

        return listOf(
            mainSource,
            CrawlSource(
                key = "apple-macbook",
                listUrl = CrawlerUrls.APPLE_MACBOOK_LIST_URL,
            ),
        )
    }
}
