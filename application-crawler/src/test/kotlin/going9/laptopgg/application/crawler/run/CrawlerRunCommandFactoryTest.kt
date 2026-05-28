package going9.laptopgg.application.crawler.run

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CrawlerRunCommandFactoryTest {
    private val endedAt = LocalDateTime.of(2026, 5, 28, 20, 30)
    private val factory = CrawlerRunCommandFactory { endedAt }

    @Test
    fun `start command owns the run start time`() {
        val command = factory.start(filterProfile = CrawlerFilterProfile.CORE, startPage = 1, limit = null)

        assertThat(command.startedAt).isEqualTo(endedAt)
        assertThat(command.endedAt).isNull()
    }

    @Test
    fun `skip locked command is completed immediately`() {
        val command = factory.skipLocked(filterProfile = CrawlerFilterProfile.CORE, startPage = 2, limit = 10)

        assertThat(command.status).isEqualTo(CrawlerRunStatusResult.SKIPPED_LOCKED)
        assertThat(command.startedAt).isEqualTo(endedAt)
        assertThat(command.endedAt).isEqualTo(endedAt)
        assertThat(command.errorMessage).contains("PostgreSQL advisory lock")
    }

    @Test
    fun `finish command stores bounded failure samples and error text`() {
        val command = factory.finish(
            runId = 1L,
            summary = CrawlerRunSummary(
                processedCount = 30,
                createdCount = 10,
                updatedCount = 15,
                detailRefreshCount = 12,
                priceOnlyUpdatedCount = 3,
                degradedCount = 2,
                failedCount = 25,
                failureSamples = (1..25).map { index -> "failure-$index" },
            ),
            status = CrawlerRunCompletionStatus.FAILED,
            errorMessage = "x".repeat(4_100),
        )

        assertThat(command.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        assertThat(command.detailRefreshCount).isEqualTo(12)
        assertThat(command.priceOnlyUpdatedCount).isEqualTo(3)
        assertThat(command.failureSamples!!.lines()).hasSize(20)
        assertThat(command.failureSamples!!.lines().last()).isEqualTo("failure-20")
        assertThat(command.errorMessage).hasSize(4_000)
        assertThat(command.endedAt).isEqualTo(endedAt)
    }

    @Test
    fun `fail command stores exception message`() {
        val command = factory.fail(2L, IllegalStateException("network timeout"))

        assertThat(command.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        assertThat(command.errorMessage).isEqualTo("network timeout")
        assertThat(command.endedAt).isEqualTo(endedAt)
    }
}
