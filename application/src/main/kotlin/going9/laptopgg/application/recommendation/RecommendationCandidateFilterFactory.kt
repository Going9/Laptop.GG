package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.recommendation.port.RecommendationCandidateFilter
import going9.laptopgg.recommendation.RecommendationUseCase
import kotlin.math.ceil

internal class RecommendationCandidateFilterFactory {
    fun create(
        request: LaptopRecommendationQuery,
        useCase: RecommendationUseCase,
        gateThreshold: Int,
    ): RecommendationCandidateFilter {
        val baseFilter = createBaseFilter(request)

        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> baseFilter.copy(
                minNotSureGateTotal = minimumRoundedAverageTotal(gateThreshold, 3),
            )
            RecommendationUseCase.OFFICE_STUDY,
            RecommendationUseCase.PORTABLE_OFFICE,
            -> baseFilter.copy(minOfficeScore = gateThreshold)
            RecommendationUseCase.BATTERY_FIRST -> baseFilter.copy(minBatteryScore = gateThreshold)
            RecommendationUseCase.CASUAL_GAME -> baseFilter.copy(minCasualGameScore = gateThreshold)
            RecommendationUseCase.ONLINE_GAME -> baseFilter.copy(minOnlineGameScore = gateThreshold)
            RecommendationUseCase.AAA_GAME -> baseFilter.copy(minAaaGameScore = gateThreshold)
            RecommendationUseCase.CREATOR -> baseFilter.copy(minCreatorScore = gateThreshold)
        }
    }

    private fun createBaseFilter(request: LaptopRecommendationQuery): RecommendationCandidateFilter {
        return when (request.resolvedScreenSizeMode()) {
            ScreenSizeMode.SELECT -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = false,
                screenSizes = request.normalizedScreenSizes(),
            )
            ScreenSizeMode.ANY -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = false,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationQuery.ALL_SELECTABLE_SCREEN_SIZES,
            )
            ScreenSizeMode.NOT_SURE -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationQuery.COMMON_SCREEN_SIZES,
            )
        }
    }

    private fun minimumRoundedAverageTotal(threshold: Int, componentCount: Int): Int {
        if (componentCount <= 0) {
            throw ApplicationInvalidStateException("componentCount must be greater than zero.")
        }
        return ceil((threshold - 0.5) * componentCount).toInt()
    }
}
