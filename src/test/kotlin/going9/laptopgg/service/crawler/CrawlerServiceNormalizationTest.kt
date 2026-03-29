package going9.laptopgg.service.crawler

import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopProfileFactory
import going9.laptopgg.service.LaptopPriceHistoryService
import going9.laptopgg.service.LaptopProfileService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime

class CrawlerServiceNormalizationTest {
    private val crawlerService = CrawlerService(
        laptopRepository = mock(LaptopRepository::class.java),
        laptopProfileService = mock(LaptopProfileService::class.java),
        laptopProfileFactory = LaptopProfileFactory(),
        laptopPriceHistoryService = mock(LaptopPriceHistoryService::class.java),
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
        val weight = crawlerService.parseWeightValue("891g")

        assertThat(weight).isCloseTo(0.891, offset(0.0001))
    }

    @Test
    fun `weight parser keeps the practical package weight when multiple values exist`() {
        val weight = crawlerService.parseWeightValue("0.87kg / 1.17kg")

        assertThat(weight).isCloseTo(1.17, offset(0.0001))
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

        val result = crawlerService.parseListPage(html)

        assertThat(result).hasSize(1)
        assertThat(result.first().detailPage).isEqualTo("https://prod.danawa.com/info/?pcode=123456&cate=112758")
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

        val result = crawlerService.extractListRequestContext(html)

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
        val laptop = sampleLaptop(lastDetailedCrawledAt = LocalDateTime.now().minusDays(5))

        val result = invokeNeedsDetailRefresh(laptop)

        assertThat(result).isFalse()
    }

    @Test
    fun `detail refresh is required for stale laptop even when specs exist`() {
        val laptop = sampleLaptop(lastDetailedCrawledAt = LocalDateTime.now().minusDays(45))

        val result = invokeNeedsDetailRefresh(laptop)

        assertThat(result).isTrue()
    }

    private fun invokeNeedsDetailRefresh(laptop: going9.laptopgg.domain.laptop.Laptop): Boolean {
        val method = crawlerService.javaClass.getDeclaredMethod("needsDetailRefresh", going9.laptopgg.domain.laptop.Laptop::class.java)
        method.setAccessible(true)
        return method.invoke(crawlerService, laptop) as Boolean
    }

    private fun sampleLaptop(lastDetailedCrawledAt: LocalDateTime?): going9.laptopgg.domain.laptop.Laptop {
        val laptop = going9.laptopgg.domain.laptop.Laptop(
            name = "테스트 노트북",
            imageUrl = "https://example.com/test.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=1&cate=112758",
            productCode = "1",
            price = 1_000_000,
            cpuManufacturer = "AMD",
            cpu = "7535HS",
            os = "윈도우11홈",
            screenSize = 15,
            resolution = "1920x1200(WUXGA)",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Radeon 660M",
            tgp = 0,
            thunderboltCount = null,
            usbCCount = 2,
            usbACount = 2,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.49,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            laptopUsage = mutableListOf(),
        )
        laptop.laptopUsage.add(going9.laptopgg.domain.laptop.LaptopUsage(usage = "사무/인강용", laptop = laptop))
        return laptop
    }
}
