package going9.laptopgg.application.recommendation

import going9.laptopgg.application.recommendation.port.RecommendationCandidateFilter

internal class RecommendationCandidateFilterFactory {
    fun create(request: LaptopRecommendationQuery): RecommendationCandidateFilter {
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
}
