package going9.laptopgg.job.runner

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlerStartupRunnerTest {
    @Test
    fun `blank default filter profile falls back to core`() {
        assertThat(CrawlerStartupRunner.normalizedFilterProfile(null)).isEqualTo("core")
        assertThat(CrawlerStartupRunner.normalizedFilterProfile("")).isEqualTo("core")
        assertThat(CrawlerStartupRunner.normalizedFilterProfile("  ")).isEqualTo("core")
    }

    @Test
    fun `provided filter profile is trimmed`() {
        assertThat(CrawlerStartupRunner.normalizedFilterProfile(" extended ")).isEqualTo("extended")
    }
}
