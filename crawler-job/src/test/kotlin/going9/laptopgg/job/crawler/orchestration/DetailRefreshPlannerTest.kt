package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopLookup
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.job.crawler.list.ProductCard
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DetailRefreshPlannerTest {
    private val fixedNow = LocalDateTime.of(2026, 5, 28, 15, 20)

    @Test
    fun `fresh complete existing product is planned for price only snapshot`() {
        val productCard = productCard("100")
        val existingLaptop = existingLaptop(productCard.productCode, lastDetailedCrawledAt = fixedNow)

        val plan = DetailRefreshPlanner.plan(
            productCards = listOf(productCard),
            existingLookup = ExistingCrawledLaptopLookup(
                byProductCode = mapOf(productCard.productCode to existingLaptop),
                byDetailPage = emptyMap(),
            ),
            now = fixedNow,
        )

        assertThat(plan.priceOnlySnapshotWorkItems.map { it.productCard }).containsExactly(productCard)
        assertThat(plan.detailRefreshWorkItems).isEmpty()
    }

    @Test
    fun `new product is planned for detail refresh`() {
        val productCard = productCard("200")

        val plan = DetailRefreshPlanner.plan(
            productCards = listOf(productCard),
            existingLookup = ExistingCrawledLaptopLookup(byProductCode = emptyMap(), byDetailPage = emptyMap()),
            now = fixedNow,
        )

        assertThat(plan.priceOnlySnapshotWorkItems).isEmpty()
        assertThat(plan.detailRefreshWorkItems.map { it.productCard }).containsExactly(productCard)
        assertThat(plan.detailRefreshWorkItems.first().existingLaptop).isNull()
    }

    @Test
    fun `stale existing product is planned for detail refresh`() {
        val productCard = productCard("300")
        val existingLaptop = existingLaptop(
            productCode = productCard.productCode,
            lastDetailedCrawledAt = fixedNow.minusDays(45),
        )

        val plan = DetailRefreshPlanner.plan(
            productCards = listOf(productCard),
            existingLookup = ExistingCrawledLaptopLookup(
                byProductCode = mapOf(productCard.productCode to existingLaptop),
                byDetailPage = emptyMap(),
            ),
            now = fixedNow,
        )

        assertThat(plan.priceOnlySnapshotWorkItems).isEmpty()
        assertThat(plan.detailRefreshWorkItems.first().existingLaptop).isEqualTo(existingLaptop)
    }

    private fun productCard(code: String): ProductCard {
        return ProductCard(
            productCode = code,
            productName = "Laptop $code",
            detailPage = "https://prod.danawa.com/info/?pcode=$code&cate=112758",
            imageUrl = "https://img.danawa.com/$code.jpg",
            price = 1_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }

    private fun existingLaptop(
        productCode: String,
        lastDetailedCrawledAt: LocalDateTime,
    ): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = productCode.toLong(),
            productCode = productCode,
            detailPage = "https://prod.danawa.com/info/?pcode=$productCode&cate=112758",
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11",
            screenSize = 14,
            resolution = "1920x1200",
            ramSize = 16,
            graphicsType = "Intel Graphics",
            storageCapacity = 512,
            batteryCapacity = 60.0,
            weight = 1.2,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usageCount = 1,
        )
    }
}
