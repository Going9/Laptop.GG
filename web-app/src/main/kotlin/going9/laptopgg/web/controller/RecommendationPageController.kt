package going9.laptopgg.web.controller

import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.view.RecommendationPageModelFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class RecommendationPageController(
    private val recommendLaptopsUseCase: RecommendLaptopsUseCase,
    private val recommendationPageModelFactory: RecommendationPageModelFactory,
) {
    @GetMapping("/recommends", "/")
    fun showRecommendationForm(model: Model): String {
        model.addAllAttributes(recommendationPageModelFactory.formAttributes())
        return "recommendation-form"
    }

    @PostMapping("/recommends")
    fun recommendLaptops(
        @ModelAttribute laptopRecommendationRequest: LaptopRecommendationRequest,
        @RequestParam(defaultValue = "0") page: Int?,
        @RequestParam(defaultValue = "10") size: Int?,
        @RequestParam(required = false) sort: List<String>?,
        model: Model,
    ): String {
        val pageQuery = pageQueryFrom(page, size, sort)
        val recommendedLaptops = recommendLaptopsUseCase
            .recommend(laptopRecommendationRequest.toQuery(), pageQuery)
        model.addAllAttributes(
            recommendationPageModelFactory.resultAttributes(
                request = laptopRecommendationRequest,
                recommendedLaptops = recommendedLaptops,
                pageQuery = pageQuery,
            ),
        )

        return "recommendation-list"
    }
}
