package going9.laptopgg.application.recommendation

import going9.laptopgg.recommendation.RecommendationUseCase

data class LaptopRecommendationQuery(
    val budget: Int = 1_500_000,
    val maxWeightKg: Double = 1.5,
    val screenSizes: List<Int> = emptyList(),
    val screenSizeMode: ScreenSizeMode? = null,
    val useCase: RecommendationUseCase = RecommendationUseCase.NOT_SURE,
) {
    fun resolvedUseCase(): RecommendationUseCase {
        return useCase
    }

    fun resolvedScreenSizeMode(): ScreenSizeMode {
        return screenSizeMode ?: if (screenSizes.isNotEmpty()) ScreenSizeMode.SELECT else ScreenSizeMode.ANY
    }

    fun normalizedScreenSizes(): List<Int> {
        return screenSizes.distinct().sorted().ifEmpty { COMMON_SCREEN_SIZES }
    }

    fun matchesScreenSize(screenSize: Int?): Boolean {
        return when (resolvedScreenSizeMode()) {
            ScreenSizeMode.SELECT -> screenSize != null && screenSize in normalizedScreenSizes()
            ScreenSizeMode.ANY -> true
            ScreenSizeMode.NOT_SURE -> screenSize == null || screenSize in COMMON_SCREEN_SIZES
        }
    }

    companion object {
        val COMMON_SCREEN_SIZES = listOf(13, 14, 15, 16)
        val ALL_SELECTABLE_SCREEN_SIZES = listOf(13, 14, 15, 16, 17, 18)
    }
}

enum class ScreenSizeMode {
    SELECT,
    ANY,
    NOT_SURE,
}
