package going9.laptopgg.job.crawler.danawa

import going9.laptopgg.application.crawler.run.CrawlerFilterProfile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DanawaCrawlSourceResolverTest {
    private val resolver = DanawaCrawlSourceResolver()

    @Test
    fun `core filter profile resolves codename source plus apple source`() {
        val resolved = resolver.resolve(CrawlerFilterProfile.CORE)
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
    fun `none filter profile resolves unfiltered notebook source`() {
        val resolved = resolver.resolve(CrawlerFilterProfile.NONE)

        assertThat(resolved.profileName)
            .isEqualTo("none")
        assertThat(resolved.sources.first().key).isEqualTo("notebook-all")
        assertThat(resolved.sources.first().attributeFilters).isEmpty()
    }
}
