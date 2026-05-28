package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.InvalidCommandException

internal class LaptopRecommendationQueryValidator {
    fun validate(request: LaptopRecommendationQuery) {
        if (request.budget <= 0) {
            throw InvalidCommandException("budget must be positive.")
        }
        if (!request.maxWeightKg.isFinite() || request.maxWeightKg <= 0.0) {
            throw InvalidCommandException("maxWeightKg must be positive.")
        }
        if (request.resolvedScreenSizeMode() == ScreenSizeMode.SELECT) {
            if (request.screenSizes.isEmpty()) {
                throw InvalidCommandException("screenSizes must not be empty when screenSizeMode is SELECT.")
            }
            val unsupportedScreenSizes = request.screenSizes
                .filterNot { it in LaptopRecommendationQuery.ALL_SELECTABLE_SCREEN_SIZES }
            if (unsupportedScreenSizes.isNotEmpty()) {
                throw InvalidCommandException("screenSizes must contain supported laptop sizes.")
            }
        }
    }
}
