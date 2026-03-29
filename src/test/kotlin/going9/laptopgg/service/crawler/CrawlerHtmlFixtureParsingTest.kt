package going9.laptopgg.service.crawler

import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopProfileFactory
import going9.laptopgg.service.LaptopPriceHistoryService
import going9.laptopgg.service.LaptopProfileService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class CrawlerHtmlFixtureParsingTest {
    private val crawlerService = CrawlerService(
        laptopRepository = mock(LaptopRepository::class.java),
        laptopProfileService = mock(LaptopProfileService::class.java),
        laptopProfileFactory = LaptopProfileFactory(),
        laptopPriceHistoryService = mock(LaptopPriceHistoryService::class.java),
    )

    @Test
    fun `list fixture keeps context and canonical product card fields`() {
        val html = readFixture("/fixtures/danawa/list-page.html")

        val requestContext = crawlerService.extractListRequestContext(html)
        val productCards = crawlerService.parseListPage(html)

        assertThat(requestContext.categoryCode).isEqualTo("758")
        assertThat(requestContext.listCount).isEqualTo("30")
        assertThat(productCards).hasSize(1)
        assertThat(productCards.first().productCode).isEqualTo("103448942")
        assertThat(productCards.first().detailPage).isEqualTo("https://prod.danawa.com/info/?pcode=103448942&cate=112758")
        assertThat(productCards.first().price).isEqualTo(1_549_000)
    }

    @Test
    fun `detail fixtures extract request context, summary fallback and spec table`() {
        val detailPageHtml = readFixture("/fixtures/danawa/detail-page.html")
        val detailSpecHtml = readFixture("/fixtures/danawa/detail-spec.html")

        val detailRequestContext = crawlerService.extractDetailRequestContext(detailPageHtml)
        val parsedSpecTable = crawlerService.parseSpecTable(detailSpecHtml)
        val summaryFallback = crawlerService.parseSummaryFallback(
            Regex("""<div class="spec_list">\s*(.*?)\s*</div>""", setOf(RegexOption.DOT_MATCHES_ALL))
                .find(detailPageHtml)
                ?.groupValues
                ?.get(1)
                .orEmpty(),
        )

        assertThat(detailRequestContext?.makerName).isEqualTo("레노버")
        assertThat(detailRequestContext?.productName).isEqualTo("아이디어패드 Slim3 15ARP10")
        assertThat(detailRequestContext?.prodType).isEqualTo("32741")
        assertThat(parsedSpecTable.values["CPU 제조사"]).isEqualTo("AMD")
        assertThat(parsedSpecTable.values["GPU 칩셋"]).isEqualTo("Radeon 660M")
        assertThat(parsedSpecTable.usages).containsExactlyInAnyOrder("사무/인강용", "학생용")
        assertThat(summaryFallback.cpuManufacturer).isEqualTo("AMD")
        assertThat(summaryFallback.cpu).isEqualTo("7535HS")
        assertThat(summaryFallback.storageCapacity).isEqualTo(512)
    }

    private fun readFixture(path: String): String {
        return requireNotNull(javaClass.getResource(path)) { "Fixture not found: $path" }.readText()
    }
}
