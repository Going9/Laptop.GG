package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.job.config.CrawlerJobProperties
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import going9.laptopgg.job.crawler.source.CrawlSource
import going9.laptopgg.job.crawler.source.CrawlSourceResolver
import going9.laptopgg.job.crawler.source.ResolvedCrawlSources
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

class CrawlerServiceTest {
    @Test
    fun `crawlAll traverses resolved sources with first requested page only once`() {
        val firstSource = crawlSource("first")
        val secondSource = crawlSource("second")
        val sourceRunner = RecordingCrawlSourceRunUseCase(
            results = listOf(
                CrawlSourceRunResult(reachedLimit = false, hitMaxListPages = false),
                CrawlSourceRunResult(reachedLimit = false, hitMaxListPages = true),
            ),
        )
        val service = crawlerService(
            sourceRunner = sourceRunner,
            sources = listOf(firstSource, secondSource),
            maxListPages = 7,
        )

        val summary = service.crawlAll(
            limit = 5,
            startPage = 3,
            filterProfile = CrawlerFilterProfile.EXTENDED,
        )

        assertThat(summary.processedCount).isEqualTo(2)
        assertThat(sourceRunner.calls.map { it.source }).containsExactly(firstSource, secondSource)
        assertThat(sourceRunner.calls.map { it.startPage }).containsExactly(3, 1)
        assertThat(sourceRunner.calls.map { it.maxListPages }).containsExactly(7, 7)
        assertThat(sourceRunner.calls.map { it.limit }).containsExactly(5, 5)
    }

    @Test
    fun `crawlAll stops traversing sources after the limit is reached`() {
        val sourceRunner = RecordingCrawlSourceRunUseCase(
            results = listOf(
                CrawlSourceRunResult(reachedLimit = true, hitMaxListPages = false),
                CrawlSourceRunResult(reachedLimit = false, hitMaxListPages = false),
            ),
        )
        val service = crawlerService(
            sourceRunner = sourceRunner,
            sources = listOf(crawlSource("first"), crawlSource("second")),
            maxListPages = 7,
        )

        service.crawlAll(limit = 1, startPage = 1, filterProfile = CrawlerFilterProfile.CORE)

        assertThat(sourceRunner.calls).hasSize(1)
    }

    @Test
    fun `crawlAll wraps fatal source failure with partial progress summary`() {
        val sourceFailure = IllegalStateException("identity conflict")
        val sourceRunner = RecordingCrawlSourceRunUseCase(
            results = emptyList(),
            failureAfterRecording = sourceFailure,
        )
        val service = crawlerService(
            sourceRunner = sourceRunner,
            sources = listOf(crawlSource("first")),
            maxListPages = 7,
        )

        val thrown = catchThrowable {
            service.crawlAll(limit = null, startPage = 1, filterProfile = CrawlerFilterProfile.CORE)
        }

        assertThat(thrown)
            .isInstanceOf(CrawlFailedWithPartialSummary::class.java)
            .hasCause(sourceFailure)
        val partialSummary = (thrown as CrawlFailedWithPartialSummary).partialSummary
        assertThat(partialSummary.processedCount).isEqualTo(1)
    }

    private fun crawlerService(
        sourceRunner: RecordingCrawlSourceRunUseCase,
        sources: List<CrawlSource>,
        maxListPages: Int,
    ): CrawlerService {
        return CrawlerService(
            crawlSourceRunner = sourceRunner,
            crawlSourceResolver = object : CrawlSourceResolver {
                override fun resolve(filterProfile: CrawlerFilterProfile): ResolvedCrawlSources {
                    return ResolvedCrawlSources(filterProfile.storageValue, sources)
                }
            },
            crawlerJobProperties = CrawlerJobProperties(maxListPages = maxListPages),
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

    private class RecordingCrawlSourceRunUseCase(
        private val results: List<CrawlSourceRunResult>,
        private val failureAfterRecording: RuntimeException? = null,
    ) : CrawlSourceRunUseCase {
        val calls = mutableListOf<CrawlSourceRunCall>()

        override fun runSource(
            crawlSource: CrawlSource,
            startPage: Int,
            maxListPages: Int,
            limit: Int?,
            seenDetailPages: MutableSet<String>,
            progress: CrawlProgress,
            detailFetchExecutor: DetailFetchExecutor,
        ): CrawlSourceRunResult {
            calls += CrawlSourceRunCall(crawlSource, startPage, maxListPages, limit)
            progress.recordProcessed(1)
            failureAfterRecording?.let { throw it }
            return results[calls.lastIndex]
        }
    }

    private data class CrawlSourceRunCall(
        val source: CrawlSource,
        val startPage: Int,
        val maxListPages: Int,
        val limit: Int?,
    )
}
