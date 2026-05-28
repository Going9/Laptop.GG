package going9.laptopgg.job.runner

import going9.laptopgg.job.config.CrawlerJobProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlerStartupRunnerTest {
    @Test
    fun `blank default filter profile falls back to core`() {
        assertThat(CrawlerJobProperties.normalizedFilterProfile(null)).isEqualTo("core")
        assertThat(CrawlerJobProperties.normalizedFilterProfile("")).isEqualTo("core")
        assertThat(CrawlerJobProperties.normalizedFilterProfile("  ")).isEqualTo("core")
    }

    @Test
    fun `provided filter profile is trimmed`() {
        assertThat(CrawlerJobProperties.normalizedFilterProfile(" extended ")).isEqualTo("extended")
    }

    @Test
    fun `non positive limit and start page are ignored`() {
        val properties = CrawlerJobProperties(limit = 0, startPage = -1)

        assertThat(properties.resolvedLimit()).isNull()
        assertThat(properties.resolvedStartPage()).isEqualTo(1)
    }

    @Test
    fun `non positive crawler tuning values fall back to defaults`() {
        val properties = CrawlerJobProperties(maxListPages = 0, detailFetchConcurrency = -1)

        assertThat(properties.resolvedMaxListPages()).isEqualTo(CrawlerJobProperties.DEFAULT_MAX_LIST_PAGES)
        assertThat(properties.resolvedDetailFetchConcurrency())
            .isEqualTo(CrawlerJobProperties.DEFAULT_DETAIL_FETCH_CONCURRENCY)
    }

    @Test
    fun `detail fetch concurrency is capped for operational safety`() {
        val properties = CrawlerJobProperties(detailFetchConcurrency = 100)

        assertThat(properties.resolvedDetailFetchConcurrency())
            .isEqualTo(CrawlerJobProperties.MAX_DETAIL_FETCH_CONCURRENCY)
    }
}
