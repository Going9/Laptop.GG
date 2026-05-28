package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.web.dto.response.CommentResponse
import going9.laptopgg.web.dto.response.LaptopDetailResponse
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.request.CommentUpdateRequest
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.view.RecommendationPageModelFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class PageController(
    private val getLaptopDetailUseCase: GetLaptopDetailUseCase,
    private val recommendLaptopsUseCase: RecommendLaptopsUseCase,
    private val manageCommentUseCase: ManageCommentUseCase,
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
        model: Model
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

    @GetMapping("/laptops/{laptopId}")
    fun showLaptopDetail(@PathVariable laptopId: Long, model: Model): String {
        val laptopDetail = LaptopDetailResponse.from(getLaptopDetailUseCase.get(laptopId))
        val commentsOfLaptop = manageCommentUseCase.listByLaptop(laptopId).map(CommentResponse::from)
        model.addAttribute("laptopDetail", laptopDetail)
        model.addAttribute("commentsOfLaptop", commentsOfLaptop)
        model.addAttribute("commentRequest", CommentRequest())
        return "laptop-detail"
    }

    @PostMapping("/comments")
    fun addComment(@ModelAttribute commentRequest: CommentRequest): String {
        manageCommentUseCase.add(commentRequest.toCommand())
        return "redirect:/laptops/${commentRequest.laptopId}"
    }

    @PostMapping("/comments/{commentId}/edit")
    fun editComment(
        @PathVariable commentId: Long,
        @ModelAttribute commentRequest: CommentRequest,
    ): String {
        manageCommentUseCase.update(
            commentId,
            CommentUpdateRequest(
                passWord = commentRequest.passWord,
                content = commentRequest.content,
            ).toCommand(),
        )
        return "redirect:/laptops/${commentRequest.laptopId}"
    }


}
