package going9.laptopgg.application.crawler.profile

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
    ): LaptopProfileSource {
        return LaptopProfileSource(
            name = "테스트 노트북",
            cpuManufacturer = "인텔",
            cpu = "Core Ultra 7",
            resolution = resolution,
            brightness = brightness,
            refreshRate = refreshRate,
            ramSize = 16,
            graphicsType = "Intel Graphics",
            tgp = 0,
            batteryCapacity = 60.0,
            weight = 1.2,
            usages = emptyList(),
        )
    }
}
