package going9.laptopgg.web.controller

import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.web.dto.response.PagedResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recommends")
internal class RecommendationController(
    private val recommendLaptopsUseCase: RecommendLaptopsUseCase,
) {

    @PostMapping()
    fun recommendLaptops(
        @RequestBody request: LaptopRecommendationRequest,
        @RequestParam(defaultValue = "0") page: Int?,
        @RequestParam(defaultValue = "10") size: Int?,
        @RequestParam parameters: MultiValueMap<String, String>,
    ): PagedResponse<LaptopRecommendationListResponse> {
        val result = recommendLaptopsUseCase
            .recommend(request.toQuery(), pageQueryFrom(page, size, parameters["sort"]))
            .map(LaptopRecommendationListResponse::from)
        return PagedResponse.from(result)
    }
}
