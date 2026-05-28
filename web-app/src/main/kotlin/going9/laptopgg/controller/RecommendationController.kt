package going9.laptopgg.controller

import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.dto.response.PagedResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recommends")
class RecommendationController(
    private val recommendLaptopsUseCase: RecommendLaptopsUseCase,
) {

    @PostMapping()
    fun recommendLaptops(
        @RequestBody request: LaptopRecommendationRequest,
        @PageableDefault(size = 10) pageable: Pageable
    ): PagedResponse<LaptopRecommendationListResponse> {
        return PagedResponse.from(recommendLaptopsUseCase.recommend(request, pageable.toPageQuery()))
    }
}
