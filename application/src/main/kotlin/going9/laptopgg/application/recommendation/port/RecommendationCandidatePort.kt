package going9.laptopgg.application.recommendation.port

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.recommendation.RecommendationUseCase

interface RecommendationCandidatePort {
    fun findRecommendationCandidatePage(query: RecommendationCandidatePageQuery): PagedResult<RecommendationCandidateRecord>
}

data class RecommendationCandidateRecord(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val price: Int,
    val weight: Double?,
    val screenSize: Int?,
    val cpu: String?,
    val graphicsType: String?,
    val resolution: String?,
    val portabilityScore: Int,
    val displayScore: Int,
    val ramScore: Int,
    val tgpScore: Int,
    val cpuPerformanceScore: Int,
    val lowPowerCpuScore: Int,
    val gpuPerformanceScore: Int,
    val gpuCreatorBonus: Int,
    val officeScore: Int,
    val batteryScore: Int,
    val casualGameScore: Int,
    val onlineGameScore: Int,
    val aaaGameScore: Int,
    val creatorScore: Int,
)

data class RecommendationCandidateFilter(
    val maxPrice: Int,
    val maxWeight: Double,
    val screenSizes: Collection<Int>,
    val screenFilterEnabled: Boolean,
    val includeUnknownScreen: Boolean,
)

data class RecommendationCandidatePageQuery(
    val filter: RecommendationCandidateFilter,
    val gateThreshold: Int,
    val budget: Int,
    val useCase: RecommendationUseCase,
    val sortMode: RecommendationCandidateSortMode,
    val pageQuery: PageQuery,
)

enum class RecommendationCandidateSortMode(
    val queryValue: String,
) {
    RECOMMENDED("recommended"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc"),
    WEIGHT_ASC("weight_asc"),
    WEIGHT_DESC("weight_desc"),
}
