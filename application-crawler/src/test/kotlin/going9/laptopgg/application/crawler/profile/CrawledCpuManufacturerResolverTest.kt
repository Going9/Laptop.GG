package going9.laptopgg.application.crawler.profile

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawledCpuManufacturerResolverTest {
    private val resolver = CrawledCpuManufacturerResolver()

    @Test
    fun `normalizes raw manufacturer names`() {
        assertThat(resolver.resolve(rawManufacturer = "Intel", productName = "", rawCpu = null)).isEqualTo("인텔")
        assertThat(resolver.resolve(rawManufacturer = "AMD", productName = "", rawCpu = null)).isEqualTo("AMD")
        assertThat(resolver.resolve(rawManufacturer = "Apple", productName = "", rawCpu = null)).isEqualTo("애플(ARM)")
        assertThat(resolver.resolve(rawManufacturer = "Qualcomm", productName = "", rawCpu = null)).isEqualTo("퀄컴")
    }

    @Test
    fun `infers apple and qualcomm manufacturers from product name or cpu token`() {
        assertThat(
            resolver.resolve(
                rawManufacturer = null,
                productName = "APPLE 맥북프로16 M5프로",
                rawCpu = null,
            ),
        ).isEqualTo("애플(ARM)")
        assertThat(
            resolver.resolve(
                rawManufacturer = null,
                productName = "Microsoft 서피스 프로11 X Elite 5G",
                rawCpu = null,
            ),
        ).isEqualTo("퀄컴")
        assertThat(
            resolver.resolve(
                rawManufacturer = null,
                productName = "ARM 노트북",
                rawCpu = "X1E-80-100",
            ),
        ).isEqualTo("퀄컴")
    }
}
