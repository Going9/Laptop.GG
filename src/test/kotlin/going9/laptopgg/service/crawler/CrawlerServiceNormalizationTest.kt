package going9.laptopgg.service.crawler

import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopProfileFactory
import going9.laptopgg.service.LaptopProfileService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.openqa.selenium.WebDriver
import org.springframework.beans.factory.ObjectProvider

class CrawlerServiceNormalizationTest {
    @Suppress("UNCHECKED_CAST")
    private val crawlerService = CrawlerService(
        webDriverProvider = mock(ObjectProvider::class.java) as ObjectProvider<WebDriver>,
        laptopRepository = mock(LaptopRepository::class.java),
        laptopProfileService = mock(LaptopProfileService::class.java),
        laptopProfileFactory = LaptopProfileFactory(),
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
}
