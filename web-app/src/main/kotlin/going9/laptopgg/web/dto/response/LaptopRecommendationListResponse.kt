package going9.laptopgg.web.dto.response

import going9.laptopgg.application.recommendation.LaptopRecommendationResult

data class LaptopRecommendationListResponse(
    val id: Long,
    val score: Double,
    val imgLink: String,
    val price: Int,
    val name: String,
    val manufacturer: String,
    val weight: Double?,
    val screenSize: Int?,
    val cpu: String?,
    val gpu: String?,
    val resolutionLabel: String?,
    val reasons: List<String>,
) {
    companion object {
        fun from(result: LaptopRecommendationResult): LaptopRecommendationListResponse {
            return LaptopRecommendationListResponse(
                id = result.id,
                score = result.score,
                imgLink = result.imgLink,
                price = result.price,
                name = result.name,
                manufacturer = result.manufacturer,
                weight = result.weight,
                screenSize = result.screenSize,
                cpu = result.cpu,
                gpu = result.gpu,
                resolutionLabel = result.resolutionLabel,
                reasons = result.reasons,
            )
        }
    }
}
