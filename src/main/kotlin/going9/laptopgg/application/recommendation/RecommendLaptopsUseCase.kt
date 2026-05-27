package going9.laptopgg.application.recommendation

import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.service.RecommendationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class RecommendLaptopsUseCase(
    private val recommendationService: RecommendationService,
) {
    fun recommend(request: LaptopRecommendationRequest, pageable: Pageable): Page<LaptopRecommendationListResponse> {
        return recommendationService.recommendLaptops(request, pageable)
    }
}
