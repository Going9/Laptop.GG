package going9.laptopgg.controller

import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.service.crawler.RecommendationServiceTMP
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/recommends")
class RecommendationController(
    private val recommendationServiceTMP: RecommendationServiceTMP
) {
    @PostMapping()
    fun recommendLaptop(@RequestBody request: LaptopRecommendationRequest): List<LaptopRecommendationListResponse> {
        return recommendationServiceTMP.recommendLaptop(request)
    }

    @PostMapping("/test")
    fun recommendLaptops(
        @RequestBody request: LaptopRecommendationRequest,
        @PageableDefault(size = 10) pageable: Pageable
    ): ResponseEntity<Page<LaptopRecommendationListResponse>> {
        val recommendations = recommendationServiceTMP.recommendLaptops(request, pageable)
        return ResponseEntity.ok(recommendations)
    }
}