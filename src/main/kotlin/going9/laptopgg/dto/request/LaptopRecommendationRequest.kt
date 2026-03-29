package going9.laptopgg.dto.request

class LaptopRecommendationRequest(
    val budget: Int = 1_500_000,
    val maxWeightKg: Double = 1.5,
    val screenSizes: List<Int> = emptyList(),
    val screenSizeMode: ScreenSizeMode? = null,
    val useCase: RecommendationUseCase? = null,
    val purpose: LegacyRecommendationPurpose? = null,
) {
    fun resolvedUseCase(): RecommendationUseCase {
        return useCase ?: purpose?.toUseCase() ?: RecommendationUseCase.NOT_SURE
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

enum class ScreenSizeMode {
    SELECT,
    ANY,
    NOT_SURE,
}

enum class RecommendationUseCase {
    NOT_SURE,
    OFFICE_STUDY,
    PORTABLE_OFFICE,
    BATTERY_FIRST,
    CASUAL_GAME,
    ONLINE_GAME,
    AAA_GAME,
    CREATOR,
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
