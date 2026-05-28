package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawlerRunPort
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TrackCrawlerRunServiceTest {
    private val crawlerRunPort = InMemoryCrawlerRunPort()
    private val service = TrackCrawlerRunService(
        crawlerRunPort = crawlerRunPort,
        transactionPort = InMemoryCrawlerTransactionPort(),
    )

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
        assertThat(persisted.status).isEqualTo(CrawlerRunStatusResult.SUCCEEDED)
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
        assertThat(persisted.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        assertThat(persisted.errorMessage).isEqualTo("network timeout")
        assertThat(persisted.endedAt).isNotNull()
    }

    private class InMemoryCrawlerRunPort : CrawlerRunPort {
        private val stored = linkedMapOf<Long, StoredCrawlerRun>()
        private var nextId = 1L

        override fun create(command: CreateCrawlerRunCommand): CrawlerRunState {
            val id = nextId++
            stored[id] = StoredCrawlerRun(
                id = id,
                filterProfile = command.filterProfile,
                startPage = command.startPage,
                limitCount = command.limitCount,
                status = command.status,
                errorMessage = command.errorMessage,
                endedAt = command.endedAt,
            )
            return stored[id]!!.toState()
        }

        override fun update(command: UpdateCrawlerRunCommand): CrawlerRunState? {
            val storedRun = stored[command.runId] ?: return null
            stored[command.runId] = storedRun.copy(
                status = command.status,
                processedCount = command.processedCount ?: storedRun.processedCount,
                createdCount = command.createdCount ?: storedRun.createdCount,
                updatedCount = command.updatedCount ?: storedRun.updatedCount,
                degradedCount = command.degradedCount ?: storedRun.degradedCount,
                failedCount = command.failedCount ?: storedRun.failedCount,
                failureSamples = command.failureSamples,
                errorMessage = command.errorMessage,
                endedAt = command.endedAt,
            )
            return stored[command.runId]!!.toState()
        }

        override fun findById(runId: Long): CrawlerRunState? {
            return stored[runId]?.toState()
        }

        fun snapshot(runId: Long): StoredCrawlerRun {
            return requireNotNull(stored[runId]) { "Crawler run not found: $runId" }
        }

        private fun StoredCrawlerRun.toState(): CrawlerRunState {
            return CrawlerRunState(id = id, status = status)
        }
    }

    private data class StoredCrawlerRun(
        val id: Long,
        val filterProfile: String,
        val startPage: Int,
        val limitCount: Int?,
        val status: CrawlerRunStatusResult,
        val processedCount: Int = 0,
        val createdCount: Int = 0,
        val updatedCount: Int = 0,
        val degradedCount: Int = 0,
        val failedCount: Int = 0,
        val failureSamples: String? = null,
        val errorMessage: String? = null,
        val endedAt: LocalDateTime? = null,
    )
}
