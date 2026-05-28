package going9.laptopgg.dto.request

import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.LegacyRecommendationPurpose
import going9.laptopgg.application.recommendation.RecommendationUseCase
import going9.laptopgg.application.recommendation.ScreenSizeMode

class LaptopRecommendationRequest(
    val budget: Int = 1_500_000,
    val maxWeightKg: Double = 1.5,
    val screenSizes: List<Int> = emptyList(),
    val screenSizeMode: ScreenSizeMode? = null,
    val useCase: RecommendationUseCase? = null,
    val purpose: LegacyRecommendationPurpose? = null,
) {
    fun toQuery(): LaptopRecommendationQuery {
        return LaptopRecommendationQuery(
            budget = budget,
            maxWeightKg = maxWeightKg,
            screenSizes = screenSizes,
            screenSizeMode = screenSizeMode,
            useCase = useCase,
            purpose = purpose,
        )
    }

    fun resolvedUseCase(): RecommendationUseCase {
        return toQuery().resolvedUseCase()
    }

    fun resolvedScreenSizeMode(): ScreenSizeMode {
        return toQuery().resolvedScreenSizeMode()
    }

    fun normalizedScreenSizes(): List<Int> {
        return toQuery().normalizedScreenSizes()
    }

    companion object {
        val COMMON_SCREEN_SIZES = LaptopRecommendationQuery.COMMON_SCREEN_SIZES
        val ALL_SELECTABLE_SCREEN_SIZES = LaptopRecommendationQuery.ALL_SELECTABLE_SCREEN_SIZES

        fun fixture(
            budget: Int = 1_500_000,
            maxWeightKg: Double = 1.5,
            screenSizes: List<Int> = emptyList(),
            screenSizeMode: ScreenSizeMode = ScreenSizeMode.ANY,
            useCase: RecommendationUseCase = RecommendationUseCase.NOT_SURE,
        ): LaptopRecommendationRequest {
            return LaptopRecommendationRequest(
                budget = budget,
                maxWeightKg = maxWeightKg,
                screenSizes = screenSizes,
                screenSizeMode = screenSizeMode,
                useCase = useCase,
            )
        }
    }
}
