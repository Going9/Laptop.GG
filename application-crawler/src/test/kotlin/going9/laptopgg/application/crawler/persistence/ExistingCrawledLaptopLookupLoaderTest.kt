package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ExistingCrawledLaptopLookupLoaderTest {
    private val laptopPort = RecordingCrawledLaptopPersistencePort()
    private val loader = ExistingCrawledLaptopLookupLoader(laptopPort)

    @Test
    fun `empty product cards do not query persistence`() {
        val lookup = loader.load(emptyList())

        assertThat(lookup.byProductCode).isEmpty()
        assertThat(lookup.byDetailPage).isEmpty()
        assertThat(laptopPort.productCodeLookups).isEmpty()
        assertThat(laptopPort.detailPageLookups).isEmpty()
    }

    @Test
    fun `loads existing laptops by product code and detail page`() {
        val detailedAt = LocalDateTime.of(2026, 5, 28, 10, 30)
        val productMatch = persistedLaptop(
            id = 10L,
            detailPage = "https://prod.danawa.com/info/?pcode=P10",
            productCode = "P10",
            lastDetailedCrawledAt = detailedAt,
            usages = listOf("business", "student"),
        )
        val detailPageMatch = persistedLaptop(
            id = 20L,
            detailPage = "https://prod.danawa.com/info/?pcode=P20",
            productCode = null,
            usages = listOf("gaming"),
        )
        laptopPort.laptopsByProductCode["P10"] = productMatch
        laptopPort.laptopsByDetailPage[detailPageMatch.detailPage] = detailPageMatch

        val lookup = loader.load(
            listOf(
                productCard(productCode = "P10", detailPage = productMatch.detailPage),
                productCard(productCode = "P10", detailPage = productMatch.detailPage),
                productCard(productCode = "P20", detailPage = detailPageMatch.detailPage),
            ),
        )

        assertThat(laptopPort.productCodeLookups).containsExactly(listOf("P10", "P20"))
        assertThat(laptopPort.detailPageLookups).containsExactly(listOf(productMatch.detailPage, detailPageMatch.detailPage))

        assertThat(lookup.byProductCode["P10"])
            .usingRecursiveComparison()
            .isEqualTo(
                ExistingCrawledLaptopSnapshot(
                    id = 10L,
                    productCode = "P10",
                    detailPage = productMatch.detailPage,
                    cpuManufacturer = "인텔",
                    cpu = "Core Ultra 7 255H",
                    os = "윈도우11홈",
                    screenSize = 14,
                    resolution = "2880x1800",
                    ramSize = 32,
                    graphicsType = "Intel Arc",
                    storageCapacity = 1024,
                    batteryCapacity = 72.0,
                    weight = 1.23,
                    lastDetailedCrawledAt = detailedAt,
                    usageCount = 2,
                ),
            )
        assertThat(lookup.byDetailPage[detailPageMatch.detailPage]?.usageCount).isEqualTo(1)
    }

    private fun productCard(productCode: String, detailPage: String): CrawledProductCardCommand {
        return CrawledProductCardCommand(
            productCode = productCode,
            productName = "Laptop $productCode",
            detailPage = detailPage,
            imageUrl = "https://img.example.com/$productCode.jpg",
            price = 1_200_000,
        )
    }

    private fun persistedLaptop(
        id: Long,
        detailPage: String,
        productCode: String?,
        lastDetailedCrawledAt: LocalDateTime? = null,
        usages: List<String> = emptyList(),
    ): PersistedCrawledLaptopSnapshot {
        return PersistedCrawledLaptopSnapshot(
            id = id,
            name = "Laptop $id",
            imageUrl = "https://img.example.com/$id.jpg",
            detailPage = detailPage,
            productCode = productCode,
            price = 1_200_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra 7 255H",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "2880x1800",
            brightness = 500,
            refreshRate = 120,
            ramSize = 32,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Arc",
            tgp = 0,
            thunderboltCount = 2,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 72.0,
            storageCapacity = 1024,
            storageSlotCount = 1,
            weight = 1.23,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usages = usages,
        )
    }

    private class RecordingCrawledLaptopPersistencePort : CrawledLaptopPersistencePort {
        val laptopsByProductCode = mutableMapOf<String, PersistedCrawledLaptopSnapshot>()
        val laptopsByDetailPage = mutableMapOf<String, PersistedCrawledLaptopSnapshot>()
        val productCodeLookups = mutableListOf<List<String>>()
        val detailPageLookups = mutableListOf<List<String>>()

        override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? = null
        override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? = null
        override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? = null

        override fun findAllByProductCodes(productCodes: Collection<String>): List<PersistedCrawledLaptopSnapshot> {
            productCodeLookups += productCodes.toList()
            return productCodes.mapNotNull(laptopsByProductCode::get)
        }

        override fun findAllByDetailPages(detailPages: Collection<String>): List<PersistedCrawledLaptopSnapshot> {
            detailPageLookups += detailPages.toList()
            return detailPages.mapNotNull(laptopsByDetailPage::get)
        }

        override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            error("create is not used by this test")
        }

        override fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            error("update is not used by this test")
        }
    }
}
