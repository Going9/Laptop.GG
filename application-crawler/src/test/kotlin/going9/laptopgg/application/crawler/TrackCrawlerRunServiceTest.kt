package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawlerRunPort
import going9.laptopgg.domain.crawler.CrawlerRun
import going9.laptopgg.domain.crawler.CrawlerRunStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TrackCrawlerRunServiceTest {
    private val crawlerRunPort = InMemoryCrawlerRunPort()
    private val service = TrackCrawlerRunService(crawlerRunPort)

    @Test
    fun `finish explicitly saves completed crawler run through port`() {
        val started = service.start(filterProfile = "core", startPage = 1, limit = 10)

        val finished = service.finish(
            runId = started.id!!,
            summary = CrawlerRunSummary(
                processedCount = 10,
                createdCount = 4,
                updatedCount = 5,
                degradedCount = 1,
                failedCount = 0,
                failureSamples = emptyList(),
            ),
            status = CrawlerRunCompletionStatus.SUCCEEDED,
        )

        val persisted = crawlerRunPort.snapshot(started.id!!)
        assertThat(finished.status).isEqualTo(CrawlerRunStatusResult.SUCCEEDED)
        assertThat(persisted.status).isEqualTo(CrawlerRunStatus.SUCCEEDED)
        assertThat(persisted.processedCount).isEqualTo(10)
        assertThat(persisted.createdCount).isEqualTo(4)
        assertThat(persisted.updatedCount).isEqualTo(5)
        assertThat(persisted.degradedCount).isEqualTo(1)
        assertThat(persisted.endedAt).isNotNull()
    }

    @Test
    fun `fail explicitly saves failed crawler run through port`() {
        val started = service.start(filterProfile = "core", startPage = 1, limit = null)

        val failed = service.fail(started.id!!, IllegalStateException("network timeout"))

        val persisted = crawlerRunPort.snapshot(started.id!!)
        assertThat(failed.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        assertThat(persisted.status).isEqualTo(CrawlerRunStatus.FAILED)
        assertThat(persisted.errorMessage).isEqualTo("network timeout")
        assertThat(persisted.endedAt).isNotNull()
    }

    private class InMemoryCrawlerRunPort : CrawlerRunPort {
        private val stored = linkedMapOf<Long, CrawlerRun>()
        private var nextId = 1L

        override fun save(crawlerRun: CrawlerRun): CrawlerRun {
            val id = crawlerRun.id ?: nextId++
            val saved = crawlerRun.copy(id = id)
            stored[id] = saved.copy()
            return saved.copy()
        }

        override fun findById(runId: Long): CrawlerRun? {
            return stored[runId]?.copy()
        }

        fun snapshot(runId: Long): CrawlerRun {
            return requireNotNull(stored[runId]) { "Crawler run not found: $runId" }
        }
    }
}
