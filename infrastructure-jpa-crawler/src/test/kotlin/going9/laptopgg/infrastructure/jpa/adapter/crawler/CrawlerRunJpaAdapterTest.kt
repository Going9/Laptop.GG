package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.run.CreateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.application.crawler.run.UpdateCrawlerRunCommand
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerRunRepository
import going9.laptopgg.persistence.model.crawler.CrawlerRun
import going9.laptopgg.persistence.model.crawler.CrawlerRunStatus
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CrawlerRunJpaAdapterTest {
    @Test
    fun `create rejects saved crawler run without generated id with explicit crawler error`() {
        val crawlerRunRepository = Mockito.mock(CrawlerRunRepository::class.java)
        Mockito.`when`(crawlerRunRepository.save(Mockito.any(CrawlerRun::class.java)))
            .thenReturn(
                CrawlerRun(
                    filterProfile = "core",
                    startPage = 1,
                    limitCount = null,
                    status = CrawlerRunStatus.RUNNING,
                    startedAt = LocalDateTime.of(2026, 5, 28, 22, 0),
                ),
            )
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        assertThatThrownBy {
            adapter.create(
                CreateCrawlerRunCommand(
                    filterProfile = CrawlerFilterProfile.CORE,
                    startPage = 1,
                    limitCount = null,
                    startedAt = LocalDateTime.of(2026, 5, 28, 22, 0),
                    status = CrawlerRunStatusResult.RUNNING,
                ),
            )
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("Persisted crawler run id")
    }

    @Test
    fun `create stores application owned started time`() {
        val startedAt = LocalDateTime.of(2026, 5, 28, 22, 10)
        val crawlerRunRepository = Mockito.mock(CrawlerRunRepository::class.java)
        Mockito.`when`(crawlerRunRepository.save(Mockito.any(CrawlerRun::class.java)))
            .thenAnswer { invocation ->
                invocation.getArgument<CrawlerRun>(0)
            }
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        assertThatThrownBy {
            adapter.create(
                CreateCrawlerRunCommand(
                    filterProfile = CrawlerFilterProfile.CORE,
                    startPage = 1,
                    limitCount = null,
                    startedAt = startedAt,
                    status = CrawlerRunStatusResult.RUNNING,
                ),
            )
        }.isInstanceOf(CrawlerInvalidStateException::class.java)

        Mockito.verify(crawlerRunRepository).save(
            Mockito.argThat { crawlerRun ->
                crawlerRun.startedAt == startedAt
            },
        )
    }

    @Test
    fun `update completion delegates to direct update query without loading crawler run entity`() {
        val endedAt = LocalDateTime.of(2026, 5, 28, 23, 0)
        val crawlerRunRepository = Mockito.mock(CrawlerRunRepository::class.java)
        Mockito.`when`(
            crawlerRunRepository.updateCompletionById(
                runId = 7L,
                status = CrawlerRunStatus.SUCCEEDED,
                processedCount = 10,
                createdCount = 4,
                updatedCount = 5,
                detailRefreshCount = 6,
                priceOnlyUpdatedCount = 2,
                degradedCount = 1,
                failedCount = 0,
                failureSamples = null,
                errorMessage = null,
                endedAt = endedAt,
            ),
        ).thenReturn(1)
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        val state = adapter.update(
            UpdateCrawlerRunCommand(
                runId = 7L,
                status = CrawlerRunStatusResult.SUCCEEDED,
                processedCount = 10,
                createdCount = 4,
                updatedCount = 5,
                detailRefreshCount = 6,
                priceOnlyUpdatedCount = 2,
                degradedCount = 1,
                failedCount = 0,
                failureSamples = null,
                errorMessage = null,
                endedAt = endedAt,
            ),
        )

        assertThat(state?.id).isEqualTo(7L)
        assertThat(state?.status).isEqualTo(CrawlerRunStatusResult.SUCCEEDED)
        Mockito.verify(crawlerRunRepository, Mockito.never()).findById(Mockito.anyLong())
        Mockito.verify(crawlerRunRepository, Mockito.never()).save(Mockito.any(CrawlerRun::class.java))
    }

    @Test
    fun `update failure delegates to direct update query without loading crawler run entity`() {
        val endedAt = LocalDateTime.of(2026, 5, 28, 23, 10)
        val crawlerRunRepository = Mockito.mock(CrawlerRunRepository::class.java)
        Mockito.`when`(
            crawlerRunRepository.updateFailureById(
                runId = 7L,
                status = CrawlerRunStatus.FAILED,
                errorMessage = "network timeout",
                endedAt = endedAt,
            ),
        ).thenReturn(1)
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        val state = adapter.update(
            UpdateCrawlerRunCommand(
                runId = 7L,
                status = CrawlerRunStatusResult.FAILED,
                errorMessage = "network timeout",
                endedAt = endedAt,
            ),
        )

        assertThat(state?.id).isEqualTo(7L)
        assertThat(state?.status).isEqualTo(CrawlerRunStatusResult.FAILED)
        Mockito.verify(crawlerRunRepository, Mockito.never()).findById(Mockito.anyLong())
        Mockito.verify(crawlerRunRepository, Mockito.never()).save(Mockito.any(CrawlerRun::class.java))
    }

    @Test
    fun `update returns null when direct update finds no crawler run row`() {
        val endedAt = LocalDateTime.of(2026, 5, 28, 23, 20)
        val crawlerRunRepository = Mockito.mock(CrawlerRunRepository::class.java)
        Mockito.`when`(
            crawlerRunRepository.updateFailureById(
                runId = 404L,
                status = CrawlerRunStatus.FAILED,
                errorMessage = "network timeout",
                endedAt = endedAt,
            ),
        ).thenReturn(0)
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        val state = adapter.update(
            UpdateCrawlerRunCommand(
                runId = 404L,
                status = CrawlerRunStatusResult.FAILED,
                errorMessage = "network timeout",
                endedAt = endedAt,
            ),
        )

        assertThat(state).isNull()
    }

    @Test
    fun `update rejects partially populated completion counts with explicit crawler error`() {
        val crawlerRunRepository = Mockito.mock(CrawlerRunRepository::class.java)
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        assertThatThrownBy {
            adapter.update(
                UpdateCrawlerRunCommand(
                    runId = 7L,
                    status = CrawlerRunStatusResult.SUCCEEDED,
                    processedCount = 10,
                    endedAt = LocalDateTime.of(2026, 5, 28, 23, 30),
                ),
            )
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("all present or all absent")
    }
}
