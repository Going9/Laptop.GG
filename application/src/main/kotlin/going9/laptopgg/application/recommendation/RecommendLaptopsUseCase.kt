package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.service.RecommendationService
import org.springframework.stereotype.Service

@Service
class RecommendLaptopsUseCase(
    private val recommendationService: RecommendationService,
) {
    fun recommend(query: LaptopRecommendationQuery, pageQuery: PageQuery): PagedResult<LaptopRecommendationResult> {
        return recommendationService.recommendLaptops(query, pageQuery)
    }
}
