package going9.laptopgg.integration.crawler

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.CrawledProductCardCommand
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.SaveResult
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.LaptopPriceHistoryRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopUsage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.profiles.active=test,crawler"])
@Transactional
class CrawlerPersistenceIntegrationTest {
    @Autowired
    lateinit var saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase

    @Autowired
    lateinit var laptopRepository: CrawlerLaptopRepository

    @Autowired
    lateinit var laptopProfileRepository: CrawlerLaptopProfileRepository

    @Autowired
    lateinit var laptopPriceHistoryRepository: LaptopPriceHistoryRepository

    @Autowired
    lateinit var recommendationScoreRepository: RecommendationScoreRepository

    @BeforeEach
    fun setUp() {
        recommendationScoreRepository.deleteAll()
        laptopPriceHistoryRepository.deleteAll()
        laptopProfileRepository.deleteAll()
        laptopRepository.deleteAll()
    }

    @Test
    fun `saveOrUpdate keeps same-name laptops separate when product codes differ`() {
        laptopRepository.save(
            laptop(
                name = "Same Name",
                detailPage = "https://example.com/original",
                productCode = "A001",
                price = 1_000_000,
            ),
        )

        val result = invokeSaveOrUpdate(
            laptop(
                name = "Same Name",
                detailPage = "https://example.com/variant",
                productCode = "B002",
                price = 1_200_000,
            ),
        )

        assertThat(result).isEqualTo(SaveResult.CREATED)
        assertThat(laptopRepository.count()).isEqualTo(2)
        assertThat(laptopRepository.findAllWithUsageByProductCodeIn(listOf("A001"))).hasSize(1)
        assertThat(laptopRepository.findAllWithUsageByProductCodeIn(listOf("B002"))).hasSize(1)
    }

    @Test
    fun `saveOrUpdate stores initial price history for new laptop`() {
        val result = invokeSaveOrUpdate(
            laptop(
                name = "Brand New",
                detailPage = "https://example.com/new",
                productCode = "NEW1",
                price = 1_490_000,
            ),
        )

        assertThat(result).isEqualTo(SaveResult.CREATED)
        assertThat(laptopPriceHistoryRepository.count()).isEqualTo(1)
        assertThat(laptopPriceHistoryRepository.findAll().single().price).isEqualTo(1_490_000)
    }

    @Test
    fun `loadExistingLookup reads existing product cards through lookup projection`() {
        val existing = laptop(
            name = "Lookup Target",
            detailPage = "https://prod.danawa.com/info/?pcode=LOOKUP",
            productCode = "LOOKUP",
            price = 1_390_000,
        )
        existing.laptopUsage += LaptopUsage(usage = "business", laptop = existing)
        existing.laptopUsage += LaptopUsage(usage = "student", laptop = existing)
        laptopRepository.save(existing)

        val lookup = saveCrawledLaptopUseCase.loadExistingLookup(
            listOf(
                CrawledProductCardCommand(
                    productCode = "LOOKUP",
                    productName = "Lookup Target",
                    detailPage = "https://prod.danawa.com/info/?pcode=LOOKUP",
                    imageUrl = "https://example.com/lookup.jpg",
                    price = 1_390_000,
                ),
            ),
        )

        assertThat(lookup.byProductCode["LOOKUP"]?.id).isEqualTo(existing.id)
        assertThat(lookup.byProductCode["LOOKUP"]?.usageCount).isEqualTo(2)
        assertThat(lookup.byDetailPage["https://prod.danawa.com/info/?pcode=LOOKUP"]?.id).isEqualTo(existing.id)
    }

    @Test
    fun `saveOrUpdate backfills product code when detail page already exists`() {
        val existing = laptopRepository.save(
            laptop(
                name = "Backfill Target",
                detailPage = "https://example.com/detail",
                productCode = null,
                price = 1_050_000,
            ),
        )

        val result = invokeSaveOrUpdate(
            laptop(
                name = "Backfill Target",
                detailPage = "https://example.com/detail",
                productCode = "P999",
                price = 1_150_000,
            ),
        )

        val refreshed = laptopRepository.findById(existing.id!!).orElseThrow()

        assertThat(result).isEqualTo(SaveResult.UPDATED)
        assertThat(laptopRepository.count()).isEqualTo(1)
        assertThat(refreshed.productCode).isEqualTo("P999")
        assertThat(refreshed.price).isEqualTo(1_150_000)
    }

    @Test
    fun `saveOrUpdate keeps existing spec values when new crawl result is sparse`() {
        val existing = laptopRepository.save(
            laptop(
                name = "Stable Model",
                detailPage = "https://prod.danawa.com/info/?pcode=555&cate=112758",
                productCode = "555",
                price = 1_390_000,
            ),
        )

        val sparseUpdate = Laptop(
            name = "Stable Model",
            imageUrl = existing.imageUrl,
            detailPage = "https://prod.danawa.com/info/?pcode=555&cate=112758",
            productCode = "555",
            price = null,
            cpuManufacturer = null,
            cpu = null,
            os = null,
            screenSize = null,
            resolution = null,
            brightness = null,
            refreshRate = null,
            ramSize = null,
            ramType = null,
            isRamReplaceable = null,
            graphicsType = null,
            tgp = null,
            thunderboltCount = null,
            usbCCount = null,
            usbACount = null,
            sdCard = null,
            isSupportsPdCharging = null,
            batteryCapacity = null,
            storageCapacity = null,
            storageSlotCount = null,
            weight = null,
            laptopUsage = mutableListOf(),
        )

        val result = invokeSaveOrUpdate(sparseUpdate)
        val refreshed = laptopRepository.findById(existing.id!!).orElseThrow()

        assertThat(result).isEqualTo(SaveResult.UNCHANGED)
        assertThat(refreshed.name).isEqualTo("Stable Model")
        assertThat(refreshed.imageUrl).contains(".jpg")
        assertThat(refreshed.price).isEqualTo(1_390_000)
        assertThat(refreshed.cpuManufacturer).isEqualTo("인텔")
        assertThat(refreshed.cpu).isEqualTo("225U")
        assertThat(refreshed.ramSize).isEqualTo(16)
        assertThat(refreshed.storageCapacity).isEqualTo(512)
        assertThat(refreshed.weight).isEqualTo(1.35)
    }

    @Test
    fun `saveListSnapshot updates price without touching existing specs`() {
        val existing = laptopRepository.save(
            laptop(
                name = "Fast Path Model",
                detailPage = "https://prod.danawa.com/info/?pcode=777&cate=112758",
                productCode = "777",
                price = 1_290_000,
            ),
        )

        val result = invokeSaveListSnapshot(
            existingLaptop = existing,
            productCard = CrawledProductCardCommand(
                productCode = "777",
                productName = "Fast Path Model",
                detailPage = "https://prod.danawa.com/info/?pcode=777&cate=112758",
                imageUrl = "https://example.com/updated.jpg",
                price = 1_190_000,
            ),
        )

        val refreshed = laptopRepository.findById(existing.id!!).orElseThrow()

        assertThat(result).isEqualTo(SaveResult.UPDATED)
        assertThat(laptopRepository.count()).isEqualTo(1)
        assertThat(refreshed.price).isEqualTo(1_190_000)
        assertThat(refreshed.imageUrl).isEqualTo("https://example.com/updated.jpg")
        assertThat(laptopPriceHistoryRepository.count()).isEqualTo(1)
        assertThat(laptopPriceHistoryRepository.findAll().single().price).isEqualTo(1_190_000)
        assertThat(refreshed.cpuManufacturer).isEqualTo("인텔")
        assertThat(refreshed.cpu).isEqualTo("225U")
        assertThat(refreshed.ramSize).isEqualTo(16)
        assertThat(refreshed.storageCapacity).isEqualTo(512)
        assertThat(refreshed.weight).isEqualTo(1.35)
    }

    private fun invokeSaveOrUpdate(laptop: Laptop): SaveResult {
        return saveCrawledLaptopUseCase.saveOrUpdateLaptop(laptop.toCrawledCommand())
    }

    private fun invokeSaveListSnapshot(
        existingLaptop: Laptop,
        productCard: CrawledProductCardCommand,
    ): SaveResult {
        return saveCrawledLaptopUseCase.saveListSnapshot(existingLaptop.id!!, productCard)
    }

    private fun laptop(
        name: String,
        detailPage: String,
        productCode: String?,
        price: Int,
    ): Laptop {
        return Laptop(
            name = name,
            imageUrl = "https://example.com/${name.hashCode()}.jpg",
            detailPage = detailPage,
            productCode = productCode,
            price = price,
            cpuManufacturer = "인텔",
            cpu = "225U",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "1920x1200(WUXGA)",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Graphics",
            tgp = 0,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.35,
            laptopUsage = mutableListOf(),
        )
    }

    private fun Laptop.toCrawledCommand(): CrawledLaptopCommand {
        return CrawledLaptopCommand(
            name = name,
            imageUrl = imageUrl,
            detailPage = detailPage,
            productCode = productCode,
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = os,
            screenSize = screenSize,
            resolution = resolution,
            brightness = brightness,
            refreshRate = refreshRate,
            ramSize = ramSize,
            ramType = ramType,
            isRamReplaceable = isRamReplaceable,
            graphicsType = graphicsType,
            tgp = tgp,
            thunderboltCount = thunderboltCount,
            usbCCount = usbCCount,
            usbACount = usbACount,
            sdCard = sdCard,
            isSupportsPdCharging = isSupportsPdCharging,
            batteryCapacity = batteryCapacity,
            storageCapacity = storageCapacity,
            storageSlotCount = storageSlotCount,
            weight = weight,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usages = laptopUsage.map { it.usage },
        )
    }
}
