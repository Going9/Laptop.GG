package going9.laptopgg.application.crawler.recommendation

import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.application.crawler.support.InMemoryCrawlerTransactionPort
import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.CpuClass
import going9.laptopgg.taxonomy.GpuClass
import going9.laptopgg.taxonomy.PortabilityTier
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationScoreServiceTest {
    private val recommendationScorePort = InMemoryRecommendationScorePort()
    private val service = RecommendationScoreService(
        recommendationScorePort = recommendationScorePort,
        transactionPort = InMemoryCrawlerTransactionPort(),
    )

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

    private fun profile(laptopId: Long): CrawledLaptopProfileState {
        return CrawledLaptopProfileState(
            laptopId = laptopId,
            profile = LaptopProfileSnapshot(
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
            ),
        )
    }

    private class InMemoryRecommendationScorePort : RecommendationScorePort {
        val saved = mutableListOf<UpsertRecommendationScoreCommand>()

        override fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>) {
            saved += scores
        }
    }
}
