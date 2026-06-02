package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.job.config.CrawlerJobProperties
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import going9.laptopgg.job.crawler.list.ListRequestContext
import going9.laptopgg.job.crawler.list.ProductListPageCrawler
import going9.laptopgg.job.crawler.list.ProductPageBatch
import going9.laptopgg.job.crawler.source.CrawlSource
import going9.laptopgg.job.crawler.source.CrawlSourceResolver
import going9.laptopgg.job.crawler.source.ResolvedCrawlSources
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CrawlSourceFailureIsolationTest {
    @Test
    fun `crawler continues with next source when list page fetch fails`() {
        val listPageCrawler = RecordingListPageCrawler(failPageFor = "broken")
        val service = crawlerService(
            listPageCrawler = listPageCrawler,
            sources = listOf(crawlSource("broken"), crawlSource("healthy")),
        )

        val summary = service.crawlAll(limit = null, startPage = 1, filterProfile = CrawlerFilterProfile.CORE)

        assertThat(summary.failedCount).isEqualTo(1)
        assertThat(summary.failureSamples).containsExactly("source=broken page=1 | list timeout")
        assertThat(listPageCrawler.pageFetches).containsExactly("broken:1", "healthy:1")
    }

    @Test
    fun `crawler continues with next source when source request context fails`() {
        val listPageCrawler = RecordingListPageCrawler(failContextFor = "broken")
        val service = crawlerService(
            listPageCrawler = listPageCrawler,
            sources = listOf(crawlSource("broken"), crawlSource("healthy")),
        )

        val summary = service.crawlAll(limit = null, startPage = 1, filterProfile = CrawlerFilterProfile.CORE)

        assertThat(summary.failedCount).isEqualTo(1)
        assertThat(summary.failureSamples).containsExactly("source=broken | context timeout")
        assertThat(listPageCrawler.createdContexts).containsExactly("broken", "healthy")
        assertThat(listPageCrawler.pageFetches).containsExactly("healthy:1")
    }

    @Test
    fun `crawler propagates interrupted list page failures`() {
        val failure = IllegalStateException("interrupted list fetch", InterruptedException("sleep interrupted"))
        val listPageCrawler = RecordingListPageCrawler(failPageFor = "broken", pageFailure = failure)
        val service = crawlerService(
            listPageCrawler = listPageCrawler,
            sources = listOf(crawlSource("broken"), crawlSource("healthy")),
        )

        try {
            assertThatThrownBy {
                service.crawlAll(limit = null, startPage = 1, filterProfile = CrawlerFilterProfile.CORE)
            }.isSameAs(failure)
        } finally {
            Thread.interrupted()
        }
        assertThat(listPageCrawler.pageFetches).containsExactly("broken:1")
    }

    private fun crawlerService(
        listPageCrawler: ProductListPageCrawler,
        sources: List<CrawlSource>,
    ): CrawlerService {
        val diagnosticsLogger = CrawlPageDiagnosticsLogger(CrawlPageDiagnosticFormatter())
        return CrawlerService(
            crawlSourceRunner = CrawlSourceRunner(
                listPageCrawler = listPageCrawler,
                crawlProductBatchProcessor = Mockito.mock(CrawlProductBatchProcessor::class.java),
                crawlPageDiagnosticsLogger = diagnosticsLogger,
                stopDecisionLogger = CrawlSourceStopDecisionLogger(diagnosticsLogger),
                crawlClock = CrawlClock { 1_000L },
            ),
            crawlSourceResolver = object : CrawlSourceResolver {
                override fun resolve(filterProfile: CrawlerFilterProfile): ResolvedCrawlSources {
                    return ResolvedCrawlSources(filterProfile.storageValue, sources)
                }
            },
            crawlerJobProperties = CrawlerJobProperties(maxListPages = 1),
            detailFetchExecutorFactory = DetailFetchExecutorFactory {
                DetailFetchExecutor.fixed(1)
            },
        )
    }

    private fun crawlSource(key: String): CrawlSource {
        return CrawlSource(
            key = key,
            listUrl = "https://example.com/$key",
        )
    }

    private class RecordingListPageCrawler(
        private val failContextFor: String? = null,
        private val failPageFor: String? = null,
        private val pageFailure: Exception = IllegalStateException("list timeout"),
    ) : ProductListPageCrawler {
        val createdContexts = mutableListOf<String>()
        val pageFetches = mutableListOf<String>()

        override fun createListRequestContext(crawlSource: CrawlSource): ListRequestContext {
            createdContexts += crawlSource.key
            if (crawlSource.key == failContextFor) {
                throw IllegalStateException("context timeout")
            }
            return listRequestContext(crawlSource.key)
        }

        override fun fetchProductPageBatch(page: Int, listRequestContext: ListRequestContext): ProductPageBatch {
            val sourceKey = listRequestContext.group
            pageFetches += "$sourceKey:$page"
            if (sourceKey == failPageFor) {
                throw pageFailure
            }
            return ProductPageBatch(
                productCards = emptyList(),
                hasNextPage = false,
                priceCompareCount = null,
                visiblePageNumbers = emptyList(),
                nextPageHint = null,
            )
        }

        private fun listRequestContext(sourceKey: String): ListRequestContext {
            return ListRequestContext(
                listUrl = "https://example.com/$sourceKey",
                listCategoryCode = "112758",
                categoryCode = "112758",
                physicsCate1 = "112",
                physicsCate2 = "758",
                physicsCate3 = "0",
                physicsCate4 = "112758",
                viewMethod = "LIST",
                sortMethod = "MinPrice",
                listCount = "90",
                group = sourceKey,
                depth = "2",
                discountProductRate = "0",
                initialPriceDisplay = "N",
                mallMinPriceDisplayYn = "Y",
                quickDeliveryCategoryYn = "N",
                quickDeliveryDisplay = "N",
                priceUnitSort = "",
                priceUnitSortOrder = "",
                simpleDescriptionDisplayYn = "Y",
                simpleDescriptionOpen = "N",
                listPackageType = "0",
                priceUnit = "0",
                priceUnitValue = "0",
                priceUnitClass = "",
                cmRecommendSort = "",
                cmRecommendSortDefault = "",
                bundleImagePreview = "N",
                packageLimit = "0",
                makerDisplayYn = "Y",
                dpgZoneUiCategory = "",
                assemblyGalleryCategory = "",
                searchAttributeValues = emptyList(),
            )
        }
    }
}
