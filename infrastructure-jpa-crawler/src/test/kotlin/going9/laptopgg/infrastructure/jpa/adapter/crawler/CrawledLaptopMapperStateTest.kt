package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.CpuClass
import going9.laptopgg.taxonomy.GpuClass
import going9.laptopgg.taxonomy.PortabilityTier
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CrawledLaptopMapperStateTest {
    @Test
    fun `laptop snapshot mapper rejects entity without persisted id with explicit crawler error`() {
        assertThatThrownBy {
            laptop().toPersistedCrawledLaptopSnapshot()
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("Persisted laptop id")
    }

    @Test
    fun `profile mapper rejects laptop reference without persisted id with explicit crawler error`() {
        val profile = CrawledLaptopProfileEntityMapper.newProfile(laptop(), profileSnapshot())

        assertThatThrownBy {
            CrawledLaptopProfileEntityMapper.toState(profile)
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("persisted laptop")
    }

    private fun laptop(): Laptop {
        return Laptop(
            name = "테스트 노트북",
            imageUrl = "https://img.danawa.com/sample.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=1&cate=112758",
            productCode = "1",
            price = 1_000_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11",
            screenSize = 14,
            resolution = "1920x1200",
            brightness = 300,
            refreshRate = 60,
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
        )
    }

    private fun profileSnapshot(): LaptopProfileSnapshot {
        return LaptopProfileSnapshot(
            cpuClass = CpuClass.PERFORMANCE,
            gpuClass = GpuClass.INTEGRATED_MAINSTREAM,
            batteryTier = BatteryTier.HIGH,
            portabilityTier = PortabilityTier.LIGHT,
            officeScore = 80,
            batteryScore = 70,
            casualGameScore = 60,
            onlineGameScore = 50,
            aaaGameScore = 40,
            creatorScore = 65,
            cpuPerformanceScore = 85,
            lowPowerCpuScore = 75,
            gpuPerformanceScore = 55,
            gpuCreatorBonus = 5,
            portabilityScore = 70,
            displayScore = 80,
            ramScore = 75,
            tgpScore = 0,
        )
    }
}
