package going9.laptopgg.service

import going9.laptopgg.domain.laptop.GpuClass
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LaptopProfileFactoryTest {
    private val laptopProfileFactory = LaptopProfileFactory()

    @Test
    fun `rtx 5070 ti is classified as a high tier gaming gpu`() {
        val laptop = laptop(
            name = "Gaming Beast 5070 Ti",
            cpuManufacturer = "인텔",
            cpu = "255H",
            graphicsType = "RTX5070 Ti",
            ramSize = 32,
            batteryCapacity = 90.0,
            tgp = 140,
            weight = 2.4,
            usages = listOf("게임용"),
        )

        val gpuInsights = laptopProfileFactory.resolveGpuInsights(laptop)
        val snapshot = laptopProfileFactory.build(laptop)

        assertThat(gpuInsights.gpuClass).isEqualTo(GpuClass.DISCRETE_HIGH)
        assertThat(snapshot.onlineGameScore).isGreaterThanOrEqualTo(55)
        assertThat(snapshot.aaaGameScore).isGreaterThanOrEqualTo(65)
    }

    @Test
    fun `arc 140t profile clears the casual game gate`() {
        val laptop = laptop(
            name = "Slim Arc Laptop",
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            ramSize = 16,
            batteryCapacity = 75.0,
            tgp = 0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )

        val snapshot = laptopProfileFactory.build(laptop)

        assertThat(snapshot.casualGameScore).isGreaterThanOrEqualTo(45)
        assertThat(snapshot.batteryScore).isGreaterThanOrEqualTo(60)
    }

    private fun laptop(
        name: String,
        cpuManufacturer: String,
        cpu: String,
        graphicsType: String,
        ramSize: Int,
        batteryCapacity: Double,
        tgp: Int,
        weight: Double,
        usages: List<String>,
    ): Laptop {
        val laptop = Laptop(
            name = name,
            imageUrl = "https://example.com/${name.hashCode()}.jpg",
            detailPage = "https://example.com/${name.hashCode()}",
            price = 1_800_000,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "2560x1600(WQXGA)",
            brightness = 400,
            refreshRate = 120,
            ramSize = ramSize,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = graphicsType,
            tgp = tgp,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = batteryCapacity,
            storageCapacity = 1024,
            storageSlotCount = 1,
            weight = weight,
            laptopUsage = mutableListOf(),
        )

        laptop.laptopUsage = usages
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()

        return laptop
    }
}
