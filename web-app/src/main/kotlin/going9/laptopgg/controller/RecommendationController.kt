package going9.laptopgg.controller

import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.dto.response.PagedResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recommends")
class RecommendationController(
    private val recommendLaptopsUseCase: RecommendLaptopsUseCase,
) {

    @PostMapping()
    fun recommendLaptops(
        @RequestBody request: LaptopRecommendationRequest,
        @RequestParam(defaultValue = "0") page: Int?,
        @RequestParam(defaultValue = "10") size: Int?,
        @RequestParam(required = false) sort: List<String>?,
    ): PagedResponse<LaptopRecommendationListResponse> {
        val result = recommendLaptopsUseCase
            .recommend(request.toQuery(), pageQueryFrom(page, size, sort))
            .map(LaptopRecommendationListResponse::from)
        return PagedResponse.from(result)
    }
}
