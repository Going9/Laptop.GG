package going9.laptopgg.application.port.out

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.domain.laptop.LaptopProfile

interface LaptopProfilePort {
    fun findRecommendationCandidates(filter: RecommendationCandidateFilter): List<LaptopProfile>
    fun findRecommendationCandidatePage(query: RecommendationCandidatePageQuery): PagedResult<LaptopProfile>
}

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
