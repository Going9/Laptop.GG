package going9.laptopgg.dto.request

class LaptopRecommendationRequest(
    val budget: Int,
    val weight: Double,
    val isTenKey: TenKeyPreference,
    val displaySize: List<Int>, // 13, 14, 15, 16, 17, 18
    val purpose: PurposeDetail,
) {
    companion object {
        fun fixture(
            budget: Int = 1000000,
            weight: Double = 2.0,
            isTenKey: TenKeyPreference = TenKeyPreference.DOSE_NOT_MATTER,
            displaySize: List<Int> = listOf(15),
            purpose: PurposeDetail = PurposeDetail.OFFICE
        ): LaptopRecommendationRequest {
            return LaptopRecommendationRequest(
                budget, weight, isTenKey, displaySize, purpose
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
    HEAVY_GAMING
}


enum class TenKeyPreference {
    NEEDED,
    NOT_NEEDED,
    DOSE_NOT_MATTER
}
