package going9.laptopgg.job.crawler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlSourceResolverTest {
    private val resolver = CrawlSourceResolver()

    @Test
    fun `core filter profile resolves codename source plus apple source`() {
        val crawlSources = resolver.resolveCrawlSources(FilterProfile.CORE)

        assertThat(crawlSources).hasSize(2)
        assertThat(crawlSources.first().key).isEqualTo("notebook-core-codename")
        assertThat(crawlSources.first().attributeFilters.map { it.name })
            .contains("팬서레이크", "고르곤 포인트", "오라이온")
        assertThat(crawlSources.last().key).isEqualTo("apple-macbook")
        assertThat(crawlSources.last().attributeFilters).isEmpty()
    }

    @Test
    fun `unknown filter profile falls back to core`() {
        assertThat(resolver.resolveFilterProfile("weird-profile"))
            .isEqualTo(FilterProfile.CORE)
        assertThat(resolver.resolveFilterProfile("none"))
            .isEqualTo(FilterProfile.NONE)
    }
}
