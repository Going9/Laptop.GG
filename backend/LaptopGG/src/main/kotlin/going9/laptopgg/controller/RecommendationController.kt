package going9.laptopgg.controller

import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationResponse
import going9.laptopgg.service.RecommendationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/recommends")
class RecommendationController(
    private val recommendationService: RecommendationService
) {

    @PostMapping()
    fun recommendLaptop(request: LaptopRecommendationRequest): List<LaptopRecommendationResponse> {
        return  recommendationService.recommendLaptop(request)
    }
}