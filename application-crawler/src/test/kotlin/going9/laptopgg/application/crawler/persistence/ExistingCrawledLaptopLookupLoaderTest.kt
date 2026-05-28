package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        val productMatch = existingLaptop(
            id = 10L,
            detailPage = "https://prod.danawa.com/info/?pcode=P10",
            productCode = "P10",
            lastDetailedCrawledAt = detailedAt,
            usageCount = 2,
        )
        val detailPageMatch = existingLaptop(
            id = 20L,
            detailPage = "https://prod.danawa.com/info/?pcode=P20",
            productCode = null,
            usageCount = 1,
        )
        laptopPort.laptops += productMatch
        laptopPort.laptops += detailPageMatch

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

    @Test
    fun `rejects one product code mapped to multiple detail pages in the same batch`() {
        assertThatThrownBy {
            loader.load(
                listOf(
                    productCard(productCode = "P10", detailPage = "https://prod.danawa.com/info/?pcode=P10"),
                    productCard(productCode = "P10", detailPage = "https://prod.danawa.com/info/?pcode=P10-variant"),
                ),
            )
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
            .hasMessageContaining("productCode")
    }

    @Test
    fun `rejects one detail page mapped to multiple product codes in the same batch`() {
        assertThatThrownBy {
            loader.load(
                listOf(
                    productCard(productCode = "P10", detailPage = "https://prod.danawa.com/info/?pcode=P10"),
                    productCard(productCode = "P10-ALT", detailPage = "https://prod.danawa.com/info/?pcode=P10"),
                ),
            )
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
            .hasMessageContaining("detailPage")
    }

    @Test
    fun `rejects duplicate persisted product code identities instead of choosing one row`() {
        laptopPort.laptops += existingLaptop(
            id = 10L,
            detailPage = "https://prod.danawa.com/info/?pcode=P10",
            productCode = "P10",
        )
        laptopPort.laptops += existingLaptop(
            id = 11L,
            detailPage = "https://prod.danawa.com/info/?pcode=P10-duplicate",
            productCode = "P10",
        )

        assertThatThrownBy {
            loader.load(listOf(productCard(productCode = "P10", detailPage = "https://prod.danawa.com/info/?pcode=P10")))
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("productCode")
            .hasMessageContaining("10")
            .hasMessageContaining("11")
    }

    @Test
    fun `rejects duplicate persisted detail page identities instead of choosing one row`() {
        laptopPort.laptops += existingLaptop(
            id = 20L,
            detailPage = "https://prod.danawa.com/info/?pcode=P20",
            productCode = "P20",
        )
        laptopPort.laptops += existingLaptop(
            id = 21L,
            detailPage = "https://prod.danawa.com/info/?pcode=P20",
            productCode = "P20-duplicate",
        )

        assertThatThrownBy {
            loader.load(listOf(productCard(productCode = "P20", detailPage = "https://prod.danawa.com/info/?pcode=P20")))
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("detailPage")
            .hasMessageContaining("20")
            .hasMessageContaining("21")
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

    private fun existingLaptop(
        id: Long,
        detailPage: String,
        productCode: String?,
        lastDetailedCrawledAt: LocalDateTime? = null,
        usageCount: Int = 0,
    ): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = id,
            detailPage = detailPage,
            productCode = productCode,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra 7 255H",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "2880x1800",
            ramSize = 32,
            graphicsType = "Intel Arc",
            batteryCapacity = 72.0,
            storageCapacity = 1024,
            weight = 1.23,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usageCount = usageCount,
        )
    }

    private class RecordingCrawledLaptopPersistencePort : CrawledLaptopPersistencePort {
        val laptops = mutableListOf<ExistingCrawledLaptopSnapshot>()
        val productCodeLookups = mutableListOf<List<String>>()
        val detailPageLookups = mutableListOf<List<String>>()

        override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? = null
        override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? = null
        override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? = null

        override fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
            productCodeLookups += productCodes.toList()
            return laptops.filter { laptop -> laptop.productCode in productCodes }
        }

        override fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
            detailPageLookups += detailPages.toList()
            return laptops.filter { laptop -> laptop.detailPage in detailPages }
        }

        override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            error("create is not used by this test")
        }

        override fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            error("update is not used by this test")
        }
    }
}
