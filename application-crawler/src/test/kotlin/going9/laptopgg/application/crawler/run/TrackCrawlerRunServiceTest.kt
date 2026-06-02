package going9.laptopgg.application.crawler.run

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort
import going9.laptopgg.application.crawler.support.InMemoryCrawlerTransactionPort
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TrackCrawlerRunServiceTest {
    private val crawlerRunPort = InMemoryCrawlerRunPort()
    private val transactionPort = InMemoryCrawlerTransactionPort()
    private val service = TrackCrawlerRunService(
        crawlerRunPort = crawlerRunPort,
        transactionPort = transactionPort,
    )

    @Test
    fun `finish explicitly saves completed crawler run through port`() {
        val started = service.start(filterProfile = CrawlerFilterProfile.CORE, startPage = 1, limit = 10)

        val finished = service.finish(
            runId = started.id,
            summary = CrawlerRunSummary(
                processedCount = 10,
                createdCount = 4,
                updatedCount = 5,
                detailRefreshCount = 6,
                priceOnlyUpdatedCount = 2,
                degradedCount = 1,
                failedCount = 0,
                failureSamples = emptyList(),
            ),
            status = CrawlerRunCompletionStatus.SUCCEEDED,
        )

        val persisted = crawlerRunPort.snapshot(started.id)
        assertThat(finished.status).isEqualTo(CrawlerRunStatusResult.SUCCEEDED)
        assertThat(persisted.status).isEqualTo(CrawlerRunStatusResult.SUCCEEDED)
        assertThat(persisted.processedCount).isEqualTo(10)
        assertThat(persisted.createdCount).isEqualTo(4)
        assertThat(persisted.updatedCount).isEqualTo(5)
        assertThat(persisted.detailRefreshCount).isEqualTo(6)
        assertThat(persisted.priceOnlyUpdatedCount).isEqualTo(2)
        assertThat(persisted.degradedCount).isEqualTo(1)
        assertThat(persisted.endedAt).isNotNull()
    }

    @Test
    fun `fail explicitly saves failed crawler run through port`() {
        val started = service.start(filterProfile = CrawlerFilterProfile.CORE, startPage = 1, limit = null)

        val failed = service.fail(started.id, IllegalStateException("network timeout"))

        val persisted = crawlerRunPort.snapshot(started.id)
        assertThat(failed.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        assertThat(persisted.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        assertThat(persisted.errorMessage).isEqualTo("network timeout")
        assertThat(persisted.endedAt).isNotNull()
    }

    @Test
    fun `finish rejects missing crawler run with explicit crawler error`() {
        assertThatThrownBy {
            service.finish(
                runId = 404L,
                summary = CrawlerRunSummary(
                    processedCount = 0,
                    createdCount = 0,
                    updatedCount = 0,
                    detailRefreshCount = 0,
                    priceOnlyUpdatedCount = 0,
                    degradedCount = 0,
                    failedCount = 0,
                    failureSamples = emptyList(),
                ),
                status = CrawlerRunCompletionStatus.SUCCEEDED,
            )
        }.isInstanceOf(CrawlerResourceNotFoundException::class.java)
    }

    @Test
    fun `start rejects invalid run request before persistence`() {
        assertThatThrownBy {
            service.start(filterProfile = CrawlerFilterProfile.CORE, startPage = 0, limit = null)
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.skipLocked(filterProfile = CrawlerFilterProfile.CORE, startPage = 1, limit = 0)
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)

        assertThat(transactionPort.writeCount).isZero()
        assertThat(crawlerRunPort.createCount).isZero()
    }

    @Test
    fun `finish rejects invalid run update before persistence`() {
        assertThatThrownBy {
            service.finish(
                runId = 0L,
                summary = successfulSummary(),
                status = CrawlerRunCompletionStatus.SUCCEEDED,
            )
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.finish(
                runId = 1L,
                summary = successfulSummary(processedCount = -1),
                status = CrawlerRunCompletionStatus.SUCCEEDED,
            )
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.fail(0L, IllegalStateException("network timeout"))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)

        assertThat(transactionPort.writeCount).isZero()
        assertThat(crawlerRunPort.updateCount).isZero()
    }

    private fun successfulSummary(processedCount: Int = 0): CrawlerRunSummary {
        return CrawlerRunSummary(
            processedCount = processedCount,
            createdCount = 0,
            updatedCount = 0,
            detailRefreshCount = 0,
            priceOnlyUpdatedCount = 0,
            degradedCount = 0,
            failedCount = 0,
            failureSamples = emptyList(),
        )
    }

    private class InMemoryCrawlerRunPort : CrawlerRunPort {
        private val stored = linkedMapOf<Long, StoredCrawlerRun>()
        private var nextId = 1L
        var createCount = 0
            private set
        var updateCount = 0
            private set

        override fun create(command: CreateCrawlerRunCommand): CrawlerRunState {
            createCount++
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
            updateCount++
            val storedRun = stored[command.runId] ?: return null
            stored[command.runId] = when (command) {
                is CompleteCrawlerRunCommand -> storedRun.copy(
                    status = command.status,
                    processedCount = command.processedCount,
                    createdCount = command.createdCount,
                    updatedCount = command.updatedCount,
                    detailRefreshCount = command.detailRefreshCount,
                    priceOnlyUpdatedCount = command.priceOnlyUpdatedCount,
                    degradedCount = command.degradedCount,
                    failedCount = command.failedCount,
                    failureSamples = command.failureSamples,
                    errorMessage = command.errorMessage,
                    endedAt = command.endedAt,
                )
                is FailCrawlerRunCommand -> storedRun.copy(
                    status = command.status,
                    failureSamples = null,
                    errorMessage = command.errorMessage,
                    endedAt = command.endedAt,
                )
            }
            return stored[command.runId]!!.toState()
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
        val filterProfile: CrawlerFilterProfile,
        val startPage: Int,
        val limitCount: Int?,
        val status: CrawlerRunStatusResult,
        val processedCount: Int = 0,
        val createdCount: Int = 0,
        val updatedCount: Int = 0,
        val detailRefreshCount: Int = 0,
        val priceOnlyUpdatedCount: Int = 0,
        val degradedCount: Int = 0,
        val failedCount: Int = 0,
        val failureSamples: String? = null,
        val errorMessage: String? = null,
        val endedAt: LocalDateTime? = null,
    )
}
