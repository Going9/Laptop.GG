package going9.laptopgg.web.dto.request

import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.recommendation.RecommendationUseCase

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
            useCase = resolvedUseCase(),
        )
    }

    fun resolvedUseCase(): RecommendationUseCase {
        return useCase ?: purpose?.toUseCase() ?: RecommendationUseCase.NOT_SURE
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
    }
}

enum class LegacyRecommendationPurpose {
    OFFICE,
    OFFICE_LOL,
    LIGHT_OFFICE,
    CREATOR,
    LIGHT_GAMING,
    MAINSTREAM_GAMING,
    HEAVY_GAMING,
    LONG_BATTERY,
    ;

    fun toUseCase(): RecommendationUseCase {
        return when (this) {
            OFFICE -> RecommendationUseCase.OFFICE_STUDY
            OFFICE_LOL -> RecommendationUseCase.CASUAL_GAME
            LIGHT_OFFICE -> RecommendationUseCase.PORTABLE_OFFICE
            CREATOR -> RecommendationUseCase.CREATOR
            LIGHT_GAMING -> RecommendationUseCase.ONLINE_GAME
            MAINSTREAM_GAMING -> RecommendationUseCase.ONLINE_GAME
            HEAVY_GAMING -> RecommendationUseCase.AAA_GAME
            LONG_BATTERY -> RecommendationUseCase.BATTERY_FIRST
        }
    }
}
