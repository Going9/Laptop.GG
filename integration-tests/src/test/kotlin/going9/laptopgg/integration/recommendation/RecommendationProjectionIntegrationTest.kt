package going9.laptopgg.integration.recommendation

import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.integration.recommendation.support.RecommendationIntegrationTestSupport
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationProjectionIntegrationTest : RecommendationIntegrationTestSupport() {
    @Test
    fun `recommendation list includes cpu gpu and friendly resolution label`() {
        fixtures.persistLaptop(
            name = "Display Friendly",
            price = 1_550_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 78.0,
            weight = 1.34,
            usages = listOf("사무/인강용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizes = listOf(14, 15, 16),
            useCase = RecommendationUseCase.OFFICE_STUDY,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))
        val laptop = result.content.first { it.name == "Display Friendly" }

        assertThat(laptop.cpu).isEqualTo("350")
        assertThat(laptop.gpu).isEqualTo("Arc 140T")
        assertThat(laptop.resolutionLabel).isEqualTo("QHD")
    }
}
