package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DisplayMetricPolicyTest {
    private val policy = DisplayMetricPolicy()

    @Test
    fun `high resolution bright fast display reaches max score`() {
        val laptop = laptop(
            resolution = "2880x1800",
            brightness = 500,
            refreshRate = 240,
        )

        assertThat(policy.displayScore(laptop)).isEqualTo(100)
    }

    @Test
    fun `missing display specs use neutral fallback scores`() {
        val laptop = laptop(
            resolution = null,
            brightness = null,
            refreshRate = null,
        )

        assertThat(policy.displayScore(laptop)).isEqualTo(51)
    }

    private fun laptop(
        resolution: String?,
        brightness: Int?,
        refreshRate: Int?,
    ): PersistedCrawledLaptopSnapshot {
        return PersistedCrawledLaptopSnapshot(
            id = 1L,
            name = "테스트 노트북",
            imageUrl = "https://example.com/laptop.jpg",
            detailPage = "https://example.com/laptop",
            productCode = "1",
            price = 1_000_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra 7",
            os = "윈도우11",
            screenSize = 14,
            resolution = resolution,
            brightness = brightness,
            refreshRate = refreshRate,
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
            weight = 1.2,
            lastDetailedCrawledAt = null,
            usages = emptyList(),
        )
    }
}
