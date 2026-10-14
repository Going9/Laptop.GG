package going9.laptopgg.application.recommendation

import going9.laptopgg.application.recommendation.port.RecommendationCandidateFilter
import going9.laptopgg.application.common.LaptopPriceFreshnessPolicy

internal class RecommendationCandidateFilterFactory {
    fun create(request: LaptopRecommendationQuery): RecommendationCandidateFilter {
        val minimumPriceCheckedAt = LaptopPriceFreshnessPolicy.minimumCheckedAt()
        return when (request.resolvedScreenSizeMode()) {
            ScreenSizeMode.SELECT -> RecommendationCandidateFilter(
                minimumPriceCheckedAt = minimumPriceCheckedAt,
                minPriceExclusive = MIN_PURCHASE_PRICE_EXCLUSIVE,
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = false,
                screenSizes = request.normalizedScreenSizes(),
            )
            ScreenSizeMode.ANY -> RecommendationCandidateFilter(
                minimumPriceCheckedAt = minimumPriceCheckedAt,
                minPriceExclusive = MIN_PURCHASE_PRICE_EXCLUSIVE,
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = false,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationQuery.ALL_SELECTABLE_SCREEN_SIZES,
            )
            ScreenSizeMode.NOT_SURE -> RecommendationCandidateFilter(
                minimumPriceCheckedAt = minimumPriceCheckedAt,
                minPriceExclusive = MIN_PURCHASE_PRICE_EXCLUSIVE,
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationQuery.COMMON_SCREEN_SIZES,
            )
        }
    }

    private companion object {
        const val MIN_PURCHASE_PRICE_EXCLUSIVE = 200_000
    }
}
