package going9.laptopgg.job.crawler.danawa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DanawaCrawlSourceResolverTest {
    private val resolver = DanawaCrawlSourceResolver()

    @Test
    fun `core filter profile resolves codename source plus apple source`() {
        val resolved = resolver.resolve("core")
        val crawlSources = resolved.sources

        assertThat(resolved.profileName).isEqualTo("core")
        assertThat(crawlSources).hasSize(2)
        assertThat(crawlSources.first().key).isEqualTo("notebook-core-codename")
        assertThat(crawlSources.first().attributeFilters.map { it.name })
            .contains("팬서레이크", "고르곤 포인트", "오라이온")
        assertThat(crawlSources.last().key).isEqualTo("apple-macbook")
        assertThat(crawlSources.last().attributeFilters).isEmpty()
    }

    @Test
    fun `unknown filter profile falls back to core`() {
        assertThat(resolver.resolve("weird-profile").profileName)
            .isEqualTo("core")
        assertThat(resolver.resolve("none").profileName)
            .isEqualTo("none")
    }
}
