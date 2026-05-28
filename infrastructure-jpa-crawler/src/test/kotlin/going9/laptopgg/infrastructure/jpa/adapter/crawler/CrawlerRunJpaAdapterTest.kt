package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.run.CreateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerRunRepository
import going9.laptopgg.persistence.model.crawler.CrawlerRun
import going9.laptopgg.persistence.model.crawler.CrawlerRunStatus
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
                ),
            )
        val adapter = CrawlerRunJpaAdapter(crawlerRunRepository)

        assertThatThrownBy {
            adapter.create(
                CreateCrawlerRunCommand(
                    filterProfile = "core",
                    startPage = 1,
                    limitCount = null,
                    status = CrawlerRunStatusResult.RUNNING,
                ),
            )
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("Persisted crawler run id")
    }
}
