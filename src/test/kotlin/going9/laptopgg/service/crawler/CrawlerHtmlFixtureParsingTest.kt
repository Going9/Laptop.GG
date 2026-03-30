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

        val requestContext = crawlerService.extractListRequestContext(
            html,
            CrawlerService.CrawlSource(
                key = "fixture",
                listUrl = "https://prod.danawa.com/list/?cate=112758",
            ),
        )
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

    @Test
    fun `page diagnostics extract count and next page hint`() {
        val html = """
            <html>
              <body>
                <ul class="tab_list_nav">
                  <li><a href="#"><strong>가격비교</strong><strong>(20,551)</strong></a></li>
                </ul>
                <div class="num_nav_wrap">
                  <div class="number_wrap">
                    <a class="num now_on">61</a>
                    <a class="num">62</a>
                    <a class="num">63</a>
                    <a class="num">64</a>
                    <a class="num">65</a>
                    <a class="num">66</a>
                    <a class="num">67</a>
                    <a class="num">68</a>
                    <a class="num">69</a>
                  </div>
                  <a class="edge_nav nav_next" onclick="javascript:movePage(70); return false;">다음 페이지</a>
                </div>
              </body>
            </html>
        """.trimIndent()

        assertThat(crawlerService.extractPriceCompareCount(html)).isEqualTo(20_551)
        assertThat(crawlerService.extractVisiblePageNumbers(html)).containsExactly(61, 62, 63, 64, 65, 66, 67, 68, 69)
        assertThat(crawlerService.extractNextPageHint(html)).isEqualTo(70)
    }

    @Test
    fun `duplicate tail stops only after repeated signature or sustained duplicate pages`() {
        assertThat(
            crawlerService.shouldStopAtDuplicateTail(
                freshProductCount = 1,
                isRepeatedPageSignature = false,
                consecutiveDuplicateOnlyPages = 3,
            ),
        ).isFalse()

        assertThat(
            crawlerService.shouldStopAtDuplicateTail(
                freshProductCount = 0,
                isRepeatedPageSignature = true,
                consecutiveDuplicateOnlyPages = 1,
            ),
        ).isTrue()

        assertThat(
            crawlerService.shouldStopAtDuplicateTail(
                freshProductCount = 0,
                isRepeatedPageSignature = false,
                consecutiveDuplicateOnlyPages = 5,
            ),
        ).isTrue()
    }

    @Test
    fun `page signature is based on detail pages in order`() {
        val cards = listOf(
            CrawlerService.ProductCard(
                productCode = "111",
                productName = "A",
                detailPage = "https://prod.danawa.com/info/?pcode=111&cate=112758",
                imageUrl = "https://img.danawa.com/a.jpg",
                price = 1000,
                cate1 = "112",
                cate2 = "758",
                cate3 = "0",
                cate4 = "112758",
            ),
            CrawlerService.ProductCard(
                productCode = "111",
                productName = "A variant",
                detailPage = "https://prod.danawa.com/info/?pcode=111&cate=112760",
                imageUrl = "https://img.danawa.com/b.jpg",
                price = 2000,
                cate1 = "112",
                cate2 = "758",
                cate3 = "0",
                cate4 = "112760",
            ),
        )

        assertThat(crawlerService.createPageSignature(cards))
            .isEqualTo(
                "https://prod.danawa.com/info/?pcode=111&cate=112758||https://prod.danawa.com/info/?pcode=111&cate=112760",
            )
    }

    @Test
    fun `core filter profile resolves codename source plus apple source`() {
        val crawlSources = crawlerService.resolveCrawlSources(CrawlerService.FilterProfile.CORE)

        assertThat(crawlSources).hasSize(2)
        assertThat(crawlSources.first().key).isEqualTo("notebook-core-codename")
        assertThat(crawlSources.first().attributeFilters.map { it.name })
            .contains("팬서레이크", "고르곤 포인트", "오라이온")
        assertThat(crawlSources.last().key).isEqualTo("apple-macbook")
        assertThat(crawlSources.last().attributeFilters).isEmpty()
    }

    @Test
    fun `list request form data keeps repeated cpu codename filters`() {
        val context = CrawlerService.ListRequestContext(
            searchAttributeValues = listOf(
                "758|6492|1137658|OR",
                "758|6492|1137661|OR",
            ),
        )

        val formData = context.toFormData(page = 1)

        assertThat(formData).contains("page" to "1")
        assertThat(formData.filter { it.first == "searchAttributeValue[]" })
            .containsExactly(
                "searchAttributeValue[]" to "758|6492|1137658|OR",
                "searchAttributeValue[]" to "758|6492|1137661|OR",
            )
    }

    @Test
    fun `unknown filter profile falls back to core`() {
        assertThat(crawlerService.resolveFilterProfile("weird-profile"))
            .isEqualTo(CrawlerService.FilterProfile.CORE)
        assertThat(crawlerService.resolveFilterProfile("none"))
            .isEqualTo(CrawlerService.FilterProfile.NONE)
    }

    private fun readFixture(path: String): String {
        return requireNotNull(javaClass.getResource(path)) { "Fixture not found: $path" }.readText()
    }
}
