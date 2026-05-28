package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import going9.laptopgg.job.config.CrawlerJobProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlerStartupRunnerTest {
    @Test
    fun `blank default filter profile falls back to core`() {
        assertThat(CrawlerFilterProfile.from(null)).isEqualTo(CrawlerFilterProfile.CORE)
        assertThat(CrawlerFilterProfile.from("")).isEqualTo(CrawlerFilterProfile.CORE)
        assertThat(CrawlerFilterProfile.from("  ")).isEqualTo(CrawlerFilterProfile.CORE)
    }

    @Test
    fun `provided filter profile is trimmed`() {
        assertThat(CrawlerFilterProfile.from(" extended ")).isEqualTo(CrawlerFilterProfile.EXTENDED)
    }

    @Test
    fun `filter profile is canonicalized before run tracking`() {
        assertThat(CrawlerFilterProfile.from(" all ")).isEqualTo(CrawlerFilterProfile.NONE)
        assertThat(CrawlerFilterProfile.from("weird-profile")).isEqualTo(CrawlerFilterProfile.CORE)
    }

    @Test
    fun `unknown filter profile keeps fallback observability`() {
        val resolution = CrawlerFilterProfile.resolve(" weird-profile ")

        assertThat(resolution.profile).isEqualTo(CrawlerFilterProfile.CORE)
        assertThat(resolution.usedDefaultForUnknownValue).isTrue()
        assertThat(resolution.rawValue).isEqualTo("weird-profile")
    }

    @Test
    fun `resolved filter profile is typed for runtime use`() {
        val properties = CrawlerJobProperties(filterProfile = " extended ")

        assertThat(properties.resolvedFilterProfile()).isEqualTo(CrawlerFilterProfile.EXTENDED)
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
