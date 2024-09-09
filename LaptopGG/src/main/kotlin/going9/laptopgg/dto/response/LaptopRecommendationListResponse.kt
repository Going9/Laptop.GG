package going9.laptopgg.dto.response

class LaptopRecommendationListResponse(
    val id: Long,
    val score: Double,
    val imgLink: String,
    val price: Int,
    val name: String,
    val manufacturer: String,
    val weight: Double,
)