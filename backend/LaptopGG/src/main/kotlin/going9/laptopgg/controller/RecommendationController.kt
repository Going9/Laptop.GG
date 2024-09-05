package going9.laptopgg.controller

import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.service.RecommendationService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/recommends")
class RecommendationController(
    private val recommendationService: RecommendationService
) {

    @PostMapping()
    fun recommendLaptop(@RequestBody request: LaptopRecommendationRequest): List<LaptopRecommendationListResponse> {
        return recommendationService.recommendLaptop(request)
    }
}