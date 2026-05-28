package going9.laptopgg.job.crawler

import going9.laptopgg.application.crawler.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.SaveCrawledLaptopUseCase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime

class CrawlerServiceNormalizationTest {
    private val laptopSnapshotMerger = LaptopSnapshotMerger(CrawledCpuModelResolver(), CrawledGraphicsModelResolver())
    private val danawaClient = DanawaClient()
    private val crawlerService = CrawlerService(
        saveCrawledLaptopUseCase = mock(SaveCrawledLaptopUseCase::class.java),
        listPageCrawler = ListPageCrawler(danawaClient),
        detailCrawler = DetailCrawler(danawaClient, laptopSnapshotMerger),
        laptopSnapshotMerger = laptopSnapshotMerger,
    )

    @Test
    fun `apple cpu fallback is extracted from product name`() {
        val cpu = crawlerService.resolveCpuModel(
            rawCpu = null,
            cpuManufacturer = "애플(ARM)",
            productName = "APPLE 맥북프로16 M5프로 18코어 CPU, 20코어 GPU 블랙",
        )

        assertThat(cpu).isEqualTo("M5 PRO")
    }

    @Test
    fun `snapdragon cpu fallback is extracted from product name`() {
        val cpu = crawlerService.resolveCpuModel(
            rawCpu = null,
            cpuManufacturer = "퀄컴",
            productName = "Microsoft 서피스 프로11 X Elite 5G",
        )

        assertThat(cpu).isEqualTo("X Elite")
    }

    @Test
    fun `weight parser supports gram values`() {
        val weight = DanawaDetailParser.parseWeightValue("891g")

        assertThat(weight).isCloseTo(0.891, offset(0.0001))
    }

    @Test
    fun `weight parser keeps the practical package weight when multiple values exist`() {
        val weight = DanawaDetailParser.parseWeightValue("0.87kg / 1.17kg")

        assertThat(weight).isCloseTo(1.17, offset(0.0001))
    }

    @Test
    fun `integrated graphics defaults missing tgp to zero`() {
        val command = laptopSnapshotMerger.createCommand(
            productCard(),
            ParsedSpecTable(
                values = mapOf(
                    "GPU 종류" to "내장그래픽",
                    "GPU 칩셋" to "Radeon 660M",
                ),
                usages = emptyList(),
            ),
            SummaryFallback(),
        )

        assertThat(command.tgp).isEqualTo(0)
    }

    @Test
    fun `discrete graphics keeps missing tgp unknown`() {
        val command = laptopSnapshotMerger.createCommand(
            productCard(),
            ParsedSpecTable(
                values = mapOf(
                    "GPU 종류" to "외장그래픽",
                    "GPU 칩셋" to "GeForce RTX4060",
                ),
                usages = emptyList(),
            ),
            SummaryFallback(),
        )

        assertThat(command.tgp).isNull()
    }

    @Test
    fun `list parser stores canonical detail url by product code`() {
        val html = """
            <ul>
              <li class="prod_item">
                <a name="productName" href="https://prod.danawa.com/info/?pcode=123456&cate=112758&bookmark=foo">테스트 노트북</a>
                <div class="prod_pricelist" data-cate="112|758|0|112758">
                  <span class="text__number">1,234,000</span>
                </div>
                <div class="thumb_image">
                  <img src="https://img.danawa.com/sample.jpg?shrink=330:330" />
                </div>
              </li>
            </ul>
        """.trimIndent()

        val result = DanawaListParser.parseListPage(html)

        assertThat(result).hasSize(1)
        assertThat(result.first().detailPage).isEqualTo("https://prod.danawa.com/info/?pcode=123456&cate=112758")
    }

    private fun productCard(): ProductCard {
        return ProductCard(
            productCode = "123456",
            productName = "테스트 노트북",
            detailPage = "https://prod.danawa.com/info/?pcode=123456&cate=112758",
            imageUrl = "https://img.danawa.com/sample.jpg",
            price = 1_234_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }

    @Test
    fun `list request context is extracted from initial html`() {
        val html = """
            <script>
                var oGlobalSetting = {
                    nCategoryCode: 758,
                    nListCategoryCode: 758,
                    nListGroup: 11,
                    nListDepth: 2,
                    sPhysicsCate1: "860",
                    sPhysicsCate2: "869",
                    sPhysicsCate3: "0",
                    sPhysicsCate4: "0"
                };
                var oExpansionContent = {
                    "sPriceCompareListType":"LIST",
                    "nPriceCompareListPackageType":"3",
                    "nPriceCompareListCount":"30",
                    "nPriceCompareListPackageLimit":"7",
                    "sInitialPriceDisplay":"N",
                    "sDiscountProductRate":"0",
                    "nPriceUnit":"0",
                    "nPriceUnitValue":"0",
                    "sPriceUnitClass":"",
                    "sCmRecommendSort":"N",
                    "sCmRecommendSortDefault":"N",
                    "sBundleImagePreview":"N",
                    "sMakerStandardDisplayStatus":"Y"
                };
            </script>
        """.trimIndent()

        val result = DanawaListParser.extractListRequestContext(
            html,
            CrawlSource(
                key = "fixture",
                listUrl = "https://prod.danawa.com/list/?cate=112758",
            ),
        )

        assertThat(result.categoryCode).isEqualTo("758")
        assertThat(result.listCategoryCode).isEqualTo("758")
        assertThat(result.group).isEqualTo("11")
        assertThat(result.depth).isEqualTo("2")
        assertThat(result.physicsCate1).isEqualTo("860")
        assertThat(result.physicsCate2).isEqualTo("869")
        assertThat(result.viewMethod).isEqualTo("LIST")
        assertThat(result.listCount).isEqualTo("30")
        assertThat(result.listPackageType).isEqualTo("3")
        assertThat(result.packageLimit).isEqualTo("7")
        assertThat(result.makerDisplayYn).isEqualTo("Y")
    }

    @Test
    fun `detail refresh is skipped for complete recently crawled laptop`() {
        val now = LocalDateTime.parse("2026-05-27T10:00:00")
        val existingLaptop = sampleExistingLaptop(lastDetailedCrawledAt = now.minusDays(5))

        val result = DetailRefreshPolicy.needsRefresh(existingLaptop, now)

        assertThat(result).isFalse()
    }

    @Test
    fun `detail refresh is required for stale laptop even when specs exist`() {
        val now = LocalDateTime.parse("2026-05-27T10:00:00")
        val existingLaptop = sampleExistingLaptop(lastDetailedCrawledAt = now.minusDays(45))

        val result = DetailRefreshPolicy.needsRefresh(existingLaptop, now)

        assertThat(result).isTrue()
    }

    private fun sampleExistingLaptop(lastDetailedCrawledAt: LocalDateTime?): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = 1L,
            productCode = "1",
            detailPage = "https://prod.danawa.com/info/?pcode=1&cate=112758",
            cpuManufacturer = "AMD",
            cpu = "7535HS",
            os = "윈도우11홈",
            screenSize = 15,
            resolution = "1920x1200(WUXGA)",
            ramSize = 16,
            graphicsType = "Radeon 660M",
            batteryCapacity = 60.0,
            storageCapacity = 512,
            weight = 1.49,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usageCount = 1,
        )
    }
}
