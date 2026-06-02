package going9.laptopgg.application.crawler.profile

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CpuTokenResolverTest {
    private val resolver = CpuTokenResolver()

    @Test
    fun `raw cpu value wins when present`() {
        assertThat(
            resolver.resolve(
                rawCpu = " Core Ultra 7 255H ",
                cpuManufacturer = "인텔",
                productName = "fallback X Elite",
            ),
        ).isEqualTo("Core Ultra 7 255H")
    }

    @Test
    fun `apple cpu token is extracted from product name with korean suffix`() {
        assertThat(
            resolver.resolve(
                rawCpu = null,
                cpuManufacturer = "애플(ARM)",
                productName = "APPLE 맥북프로16 M5프로 18코어 CPU",
            ),
        ).isEqualTo("M5 PRO")
    }

    @Test
    fun `snapdragon cpu token is extracted from product name`() {
        assertThat(
            resolver.resolve(
                rawCpu = null,
                cpuManufacturer = "퀄컴",
                productName = "Microsoft 서피스 프로11 X Elite 5G",
            ),
        ).isEqualTo("X Elite")
    }

    @Test
    fun `normalization keeps apple suffix spacing stable`() {
        assertThat(resolver.normalize("m5프로")).isEqualTo("M5 PRO")
    }
}
