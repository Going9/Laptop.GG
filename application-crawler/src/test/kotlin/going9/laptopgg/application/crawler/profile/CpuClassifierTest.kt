package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.CpuClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CpuClassifierTest {
    private val factory = LaptopProfileFactory()

    @Test
    fun `Intel model tier and generation distinguish CPUs with the same suffix`() {
        assertThat(cpu("i7-1355U").performanceScore).isGreaterThan(cpu("i3-1315U").performanceScore)
        assertThat(cpu("i5-1335U").performanceScore).isGreaterThan(cpu("i5-8250U").performanceScore)
        assertThat(cpu("155H").performanceScore).isGreaterThan(cpu("125H").performanceScore)
        assertThat(cpu("255H").performanceScore).isGreaterThan(cpu("155H").performanceScore)
        assertThat(cpu("258V").lowPowerScore).isGreaterThan(cpu("255H").lowPowerScore)
        assertThat(cpu("255H").performanceScore).isGreaterThan(cpu("258V").performanceScore)
    }

    @Test
    fun `Ryzen mobile architecture matters more than marketing year`() {
        assertThat(cpu("7530U", "AMD").performanceScore).isGreaterThan(cpu("7520U", "AMD").performanceScore)
        assertThat(cpu("7840U", "AMD").performanceScore).isGreaterThan(cpu("7730U", "AMD").performanceScore)
        assertThat(cpu("5625U", "AMD").performanceScore).isGreaterThan(cpu("5500U", "AMD").performanceScore)
    }

    @Test
    fun `AI HX is recognized before high power HX and prefixed and bare tokens agree`() {
        val ai = cpu("Ryzen AI 9 HX 370", "AMD")
        assertThat(ai.performanceScore).isEqualTo(cpu("370", "AMD").performanceScore)
        assertThat(ai.lowPowerScore).isEqualTo(cpu("PRO 370", "AMD").lowPowerScore)
        assertThat(ai.lowPowerScore).isGreaterThan(cpu("7945HX", "AMD").lowPowerScore)
        assertThat(ai.cpuClass).isEqualTo(CpuClass.BALANCED)
        assertThat(cpu("350", "AMD").performanceScore).isGreaterThan(cpu("340", "AMD").performanceScore)
        assertThat(cpu("445", "AMD").performanceScore).isLessThan(cpu("470", "AMD").performanceScore)
        assertThat(cpu("395", "AMD").performanceScore).isGreaterThan(ai.performanceScore)
        assertThat(cpu("395", "AMD").lowPowerScore).isLessThan(ai.lowPowerScore)
        assertThat(cpu("260", "AMD").performanceScore).isGreaterThan(cpu("220", "AMD").performanceScore)
    }

    @Test
    fun `CPU characteristics flow into office battery gaming and creator profiles`() {
        val low = factory.build(laptop("i3-1315U"))
        val high = factory.build(laptop("i7-1355U"))
        assertThat(high.officeScore).isGreaterThan(low.officeScore)
        assertThat(high.creatorScore).isGreaterThan(low.creatorScore)
        assertThat(high.casualGameScore).isGreaterThan(low.casualGameScore)
        assertThat(factory.build(laptop("258V")).batteryScore)
            .isGreaterThan(factory.build(laptop("255H")).batteryScore)
    }

    @Test
    fun `unknown Apple and Snapdragon retain existing classification and bounded scores`() {
        assertThat(cpu(null).cpuClass).isEqualTo(CpuClass.UNKNOWN)
        assertThat(cpu("M4 PRO", "애플").cpuClass).isEqualTo(CpuClass.PERFORMANCE)
        assertThat(cpu("X1E-78-100", "퀄컴").lowPowerScore).isEqualTo(90)
        assertThat(cpu("7945HX3D", "AMD").performanceScore).isBetween(0, 100)
        assertThat(cpu("unrecognized").performanceScore).isBetween(0, 100)
    }

    private fun cpu(token: String?, manufacturer: String = "인텔") =
        factory.resolveCpuInsights(laptop(token, manufacturer))

    private fun laptop(token: String?, manufacturer: String = "인텔") = LaptopProfileSource(
        name = "Test laptop", cpuManufacturer = manufacturer, cpu = token,
        resolution = "1920x1080", brightness = 300, refreshRate = 60, ramSize = 16,
        graphicsType = "Radeon 780M", tgp = null, batteryCapacity = 60.0, weight = 1.4, usages = emptyList(),
    )
}
