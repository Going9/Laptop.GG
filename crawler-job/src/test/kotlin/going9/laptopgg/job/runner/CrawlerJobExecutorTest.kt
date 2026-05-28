package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerLockResult
import going9.laptopgg.application.crawler.run.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.CrawlerRunRecord
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.application.crawler.run.CrawlerRunSummary
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.job.crawler.orchestration.CrawlSummary
import going9.laptopgg.job.crawler.orchestration.CrawlerService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CrawlerJobExecutorTest {
    @Test
    fun `successful crawler run is tracked and returns zero`() {
        val crawlerService = Mockito.mock(CrawlerService::class.java)
        Mockito.`when`(crawlerService.crawlAll(5, 2, "core"))
            .thenReturn(crawlSummary(failedCount = 0))
        val trackUseCase = RecordingTrackCrawlerRunUseCase()
        val executor = CrawlerJobExecutor(
            crawlerService = crawlerService,
            crawlerRunLockUseCase = RecordingCrawlerRunLockUseCase(acquired = true),
            trackCrawlerRunUseCase = trackUseCase,
            crawlerJobSummaryLogger = CrawlerJobSummaryLogger(),
        )

        val exitCode = executor.execute(CrawlerJobRequest(limit = 5, startPage = 2, filterProfile = "core"))

        assertThat(exitCode).isZero()
        assertThat(trackUseCase.startedRequests).containsExactly(CrawlerJobRequest(limit = 5, startPage = 2, filterProfile = "core"))
        assertThat(trackUseCase.finishedStatus).isEqualTo(CrawlerRunCompletionStatus.SUCCEEDED)
        assertThat(trackUseCase.finishedSummary?.failedCount).isZero()
        assertThat(trackUseCase.finishedSummary?.detailRefreshCount).isEqualTo(4)
        assertThat(trackUseCase.finishedSummary?.priceOnlyUpdatedCount).isEqualTo(2)
    }

    @Test
    fun `crawler run with failed items records failed completion and returns one`() {
        val crawlerService = Mockito.mock(CrawlerService::class.java)
        Mockito.`when`(crawlerService.crawlAll(null, 1, "extended"))
            .thenReturn(crawlSummary(failedCount = 2, failureSamples = listOf("sample failure")))
        val trackUseCase = RecordingTrackCrawlerRunUseCase()
        val executor = CrawlerJobExecutor(
            crawlerService = crawlerService,
            crawlerRunLockUseCase = RecordingCrawlerRunLockUseCase(acquired = true),
            trackCrawlerRunUseCase = trackUseCase,
            crawlerJobSummaryLogger = CrawlerJobSummaryLogger(),
        )

        val exitCode = executor.execute(CrawlerJobRequest(limit = null, startPage = 1, filterProfile = "extended"))

        assertThat(exitCode).isEqualTo(1)
        assertThat(trackUseCase.finishedStatus).isEqualTo(CrawlerRunCompletionStatus.FAILED)
        assertThat(trackUseCase.finishedSummary?.failureSamples).containsExactly("sample failure")
        assertThat(trackUseCase.finishedErrorMessage).isEqualTo("Crawler finished with 2 failed item(s).")
    }

    @Test
    fun `locked crawler run is recorded as skipped without crawling`() {
        val crawlerService = Mockito.mock(CrawlerService::class.java)
        val trackUseCase = RecordingTrackCrawlerRunUseCase()
        val executor = CrawlerJobExecutor(
            crawlerService = crawlerService,
            crawlerRunLockUseCase = RecordingCrawlerRunLockUseCase(acquired = false),
            trackCrawlerRunUseCase = trackUseCase,
            crawlerJobSummaryLogger = CrawlerJobSummaryLogger(),
        )

        val exitCode = executor.execute(CrawlerJobRequest(limit = 3, startPage = 4, filterProfile = "core"))

        assertThat(exitCode).isZero()
        assertThat(trackUseCase.skippedRequests).containsExactly(CrawlerJobRequest(limit = 3, startPage = 4, filterProfile = "core"))
        Mockito.verifyNoInteractions(crawlerService)
    }

    @Test
    fun `crawler exception records failed run and returns one`() {
        val crawlerService = Mockito.mock(CrawlerService::class.java)
        val exception = IllegalStateException("boom")
        Mockito.`when`(crawlerService.crawlAll(null, 1, "core")).thenThrow(exception)
        val trackUseCase = RecordingTrackCrawlerRunUseCase()
        val executor = CrawlerJobExecutor(
            crawlerService = crawlerService,
            crawlerRunLockUseCase = RecordingCrawlerRunLockUseCase(acquired = true),
            trackCrawlerRunUseCase = trackUseCase,
            crawlerJobSummaryLogger = CrawlerJobSummaryLogger(),
        )

        val exitCode = executor.execute(CrawlerJobRequest(limit = null, startPage = 1, filterProfile = "core"))

        assertThat(exitCode).isEqualTo(1)
        assertThat(trackUseCase.failedException).isSameAs(exception)
    }

    @Test
    fun `crawler fatal error is propagated without failed run tracking`() {
        val crawlerService = Mockito.mock(CrawlerService::class.java)
        val error = NoClassDefFoundError("crawler linkage")
        Mockito.`when`(crawlerService.crawlAll(null, 1, "core")).thenThrow(error)
        val trackUseCase = RecordingTrackCrawlerRunUseCase()
        val executor = CrawlerJobExecutor(
            crawlerService = crawlerService,
            crawlerRunLockUseCase = RecordingCrawlerRunLockUseCase(acquired = true),
            trackCrawlerRunUseCase = trackUseCase,
            crawlerJobSummaryLogger = CrawlerJobSummaryLogger(),
        )

        assertThatThrownBy {
            executor.execute(CrawlerJobRequest(limit = null, startPage = 1, filterProfile = "core"))
        }.isSameAs(error)
        assertThat(trackUseCase.failedException).isNull()
    }

    private fun crawlSummary(
        failedCount: Int,
        failureSamples: List<String> = emptyList(),
    ): CrawlSummary {
        return CrawlSummary(
            processedCount = 10,
            createdCount = 3,
            updatedCount = 7,
            detailRefreshCount = 4,
            priceOnlyUpdatedCount = 2,
            degradedCount = 0,
            degradedSamples = emptyList(),
            failedCount = failedCount,
            failureSamples = failureSamples,
        )
    }

    private class RecordingCrawlerRunLockUseCase(
        private val acquired: Boolean,
    ) : CrawlerRunLockUseCase {
        override fun <T> runLocked(block: () -> T): CrawlerLockResult<T> {
            if (!acquired) {
                return CrawlerLockResult(acquired = false, value = null)
            }
            return CrawlerLockResult(acquired = true, value = block())
        }
    }

    private class RecordingTrackCrawlerRunUseCase : TrackCrawlerRunUseCase {
        val startedRequests = mutableListOf<CrawlerJobRequest>()
        val skippedRequests = mutableListOf<CrawlerJobRequest>()
        var finishedSummary: CrawlerRunSummary? = null
            private set
        var finishedStatus: CrawlerRunCompletionStatus? = null
            private set
        var finishedErrorMessage: String? = null
            private set
        var failedException: Exception? = null
            private set

        override fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
            startedRequests += CrawlerJobRequest(limit = limit, startPage = startPage, filterProfile = filterProfile)
            return CrawlerRunRecord(id = RUN_ID, status = CrawlerRunStatusResult.RUNNING)
        }

        override fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
            skippedRequests += CrawlerJobRequest(limit = limit, startPage = startPage, filterProfile = filterProfile)
            return CrawlerRunRecord(id = RUN_ID, status = CrawlerRunStatusResult.SKIPPED_LOCKED)
        }

        override fun finish(
            runId: Long,
            summary: CrawlerRunSummary,
            status: CrawlerRunCompletionStatus,
            errorMessage: String?,
        ): CrawlerRunRecord {
            finishedSummary = summary
            finishedStatus = status
            finishedErrorMessage = errorMessage
            return CrawlerRunRecord(
                id = runId,
                status = when (status) {
                    CrawlerRunCompletionStatus.SUCCEEDED -> CrawlerRunStatusResult.SUCCEEDED
                    CrawlerRunCompletionStatus.FAILED -> CrawlerRunStatusResult.FAILED
                },
            )
        }

        override fun fail(runId: Long, exception: Exception): CrawlerRunRecord {
            failedException = exception
            return CrawlerRunRecord(id = runId, status = CrawlerRunStatusResult.FAILED)
        }

        private companion object {
            const val RUN_ID = 11L
        }
    }
}
