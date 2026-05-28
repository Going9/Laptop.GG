package going9.laptopgg.application.port.out

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.common.PageQuery

interface LaptopProfilePort {
    fun findRecommendationCandidates(filter: RecommendationCandidateFilter): List<RecommendationCandidateRecord>
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
    val minOfficeScore: Int? = null,
    val minBatteryScore: Int? = null,
    val minCasualGameScore: Int? = null,
    val minOnlineGameScore: Int? = null,
    val minAaaGameScore: Int? = null,
    val minCreatorScore: Int? = null,
    val minNotSureGateTotal: Int? = null,
)

data class RecommendationCandidatePageQuery(
    val filter: RecommendationCandidateFilter,
    val gateThreshold: Int,
    val budget: Int,
    val useCase: String,
    val sortMode: String,
    val pageQuery: PageQuery,
)
