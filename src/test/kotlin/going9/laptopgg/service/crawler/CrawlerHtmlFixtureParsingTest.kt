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

    @Test
    fun `list parsing keeps cards when product code matches but detail page differs`() {
        val html = """
            <html>
              <body>
                <ul>
                  <li class="prod_item">
                    <a name="productName" href="https://prod.danawa.com/info/?pcode=111&cate=112758">모델 A</a>
                    <div class="thumb_image"><img src="https://img.danawa.com/a.jpg" /></div>
                    <div class="prod_pricelist" data-cate="112|758|0|112758"><span class="text__number">1,000</span></div>
                  </li>
                  <li class="prod_item">
                    <a name="productName" href="https://prod.danawa.com/info/?pcode=111&cate=112760">모델 A 변형</a>
                    <div class="thumb_image"><img src="https://img.danawa.com/b.jpg" /></div>
                    <div class="prod_pricelist" data-cate="112|758|0|112760"><span class="text__number">2,000</span></div>
                  </li>
                </ul>
              </body>
            </html>
        """.trimIndent()

        val productCards = crawlerService.parseListPage(html)

        assertThat(productCards).hasSize(2)
        assertThat(productCards.map { it.detailPage }).containsExactly(
            "https://prod.danawa.com/info/?pcode=111&cate=112758",
            "https://prod.danawa.com/info/?pcode=111&cate=112760",
        )
    }

    @Test
    fun `page navigation detects a next page when navigation block exposes it`() {
        val html = """
            <html>
              <body>
                <div class="num_nav_wrap">
                  <div class="number_wrap">
                    <a class="num now_on">1</a>
                    <a class="num">2</a>
                  </div>
                  <a class="edge_nav nav_next" href="#">다음 페이지</a>
                </div>
              </body>
            </html>
        """.trimIndent()

        assertThat(crawlerService.hasNextPage(html, currentPage = 1)).isTrue()
        assertThat(crawlerService.hasNextPage(html, currentPage = 2)).isTrue()
    }

    private fun readFixture(path: String): String {
        return requireNotNull(javaClass.getResource(path)) { "Fixture not found: $path" }.readText()
    }
}
