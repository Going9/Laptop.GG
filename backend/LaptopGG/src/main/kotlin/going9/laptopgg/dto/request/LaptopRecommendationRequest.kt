package going9.laptopgg.dto.request

class LaptopRecommendationRequest(
    val budget: Int,
    val weight: Double,
    val isTenKey: TenKeyPreference,
    val displaySize: List<Int>, // 13, 14, 15, 16, 17, 18
    val purpose: PurposeDetail,
) {
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

