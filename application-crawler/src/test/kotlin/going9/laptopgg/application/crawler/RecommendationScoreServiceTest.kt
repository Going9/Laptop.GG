package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.RecommendationScorePort
import going9.laptopgg.domain.laptop.BatteryTier
import going9.laptopgg.domain.laptop.CpuClass
import going9.laptopgg.domain.laptop.GpuClass
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.domain.laptop.PortabilityTier
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationScoreServiceTest {
    private val recommendationScorePort = InMemoryRecommendationScorePort()
    private val service = RecommendationScoreService(recommendationScorePort)

    @Test
    fun `refresh saves one score command per use case`() {
        service.refreshScores(profile(laptopId = 30L))

        assertThat(recommendationScorePort.saved).hasSize(RecommendationUseCase.entries.size)
        assertThat(recommendationScorePort.saved.map { it.laptopId }.distinct()).containsExactly(30L)
        assertThat(recommendationScorePort.saved.map { it.useCase })
            .containsExactlyElementsOf(RecommendationUseCase.entries.map { it.name })
        assertThat(recommendationScorePort.saved).allSatisfy { command ->
            assertThat(command.gateScore).isGreaterThanOrEqualTo(0)
            assertThat(command.staticScore).isGreaterThanOrEqualTo(0.0)
            assertThat(command.updatedAt).isNotNull()
        }
    }

    private fun profile(laptopId: Long): LaptopProfile {
        return LaptopProfile(
            laptop = laptop(laptopId),
            cpuClass = CpuClass.PERFORMANCE,
            gpuClass = GpuClass.DISCRETE_HIGH,
            batteryTier = BatteryTier.HIGH,
            portabilityTier = PortabilityTier.LIGHT,
            officeScore = 80,
            batteryScore = 70,
            casualGameScore = 75,
            onlineGameScore = 82,
            aaaGameScore = 78,
            creatorScore = 76,
            cpuPerformanceScore = 84,
            lowPowerCpuScore = 45,
            gpuPerformanceScore = 88,
            gpuCreatorBonus = 8,
            portabilityScore = 65,
            displayScore = 80,
            ramScore = 75,
            tgpScore = 70,
        )
    }

    private fun laptop(id: Long): Laptop {
        return Laptop(
            name = "Laptop $id",
            imageUrl = "https://example.com/$id.jpg",
            detailPage = "https://example.com/$id",
            productCode = id.toString(),
            price = 1_500_000,
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
            graphicsType = "RTX 5070",
            tgp = 100,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 70.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.4,
            id = id,
        )
    }

    private class InMemoryRecommendationScorePort : RecommendationScorePort {
        val saved = mutableListOf<UpsertRecommendationScoreCommand>()

        override fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>) {
            saved += scores
        }
    }
}
