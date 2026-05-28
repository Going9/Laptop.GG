package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.service.RecommendationService
import org.springframework.stereotype.Service

@Service
class RecommendLaptopsUseCase(
    private val recommendationService: RecommendationService,
) {
    fun recommend(request: LaptopRecommendationRequest, pageQuery: PageQuery): PagedResult<LaptopRecommendationListResponse> {
        return recommendationService.recommendLaptops(request, pageQuery)
    }
}
