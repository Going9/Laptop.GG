package going9.laptopgg.dto.response

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
)
