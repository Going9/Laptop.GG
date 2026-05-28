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
}
