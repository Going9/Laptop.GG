package going9.laptopgg.dto.request

class LaptopRecommendationRequest(
    val budget: Int,
    val weight: Double,
    val displaySize: List<Int>, // 13, 14, 15, 16, 17, 18
    val purpose: PurposeDetail,
) {
    companion object {
        fun fixture(
            budget: Int = 1500000,
            weight: Double = 1.5,
            displaySize: List<Int> = listOf(13, 14, 15, 16),
            purpose: PurposeDetail = PurposeDetail.OFFICE
        ): LaptopRecommendationRequest {
            return LaptopRecommendationRequest(
                budget, weight, displaySize, purpose
            )
        }
    }
}

enum class PurposeDetail {
    OFFICE,
    OFFICE_LOL,
    LIGHT_OFFICE,
    CREATOR,
    LIGHT_GAMING,
    MAINSTREAM_GAMING,
    HEAVY_GAMING,
    LONG_BATTERY,
}


