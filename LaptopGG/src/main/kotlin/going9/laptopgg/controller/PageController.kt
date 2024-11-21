package going9.laptopgg.controller

import going9.laptopgg.dto.request.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate

@Controller
class PageController(
    @Value("\${spring.api.base-url:http://localhost:8080}")
    private val apiBaseUrl: String,
    private val restTemplate: RestTemplate,
    private val laptopController: LaptopController,
    private val recommendationController: RecommendationController,
    private val commentController: CommentController,
) {

    @GetMapping("/recommends", "/")
    fun showRecommendationForm(model: Model): String {
        model.addAttribute("laptopRecommendationRequest", LaptopRecommendationRequest.fixture())
        model.addAttribute("displaySizeList", listOf(13, 14, 15, 16, 17, 18))
        return "recommendation-form"
    }

    // 추천 결과 화면
    @PostMapping("/recommends")
    fun recommendLaptops(
        @ModelAttribute laptopRecommendationRequest: LaptopRecommendationRequest,
        @PageableDefault(size = 10) pageable: Pageable,
        model: Model
    ): String {
        // 서비스 호출
        val recommendedLaptops = recommendationController.recommendLaptops(laptopRecommendationRequest, pageable)

        // 모델에 데이터 추가
        model.addAttribute("laptopRecommendationRequest", laptopRecommendationRequest)
        model.addAttribute("recommendedLaptops", recommendedLaptops.content)
        model.addAttribute("totalPages", recommendedLaptops.totalPages)
        model.addAttribute("currentPage", recommendedLaptops.number + 1)

        return "recommendation-list"
    }

    @GetMapping("/laptops/{laptopId}")
    fun showLaptopDetail(@PathVariable laptopId: Long, model: Model): String {
        val laptopDetail = laptopController.getLaptopDetail(laptopId)
        val commentsOfLaptop = commentController.getAllComments(laptopId)
        model.addAttribute("laptopDetail", laptopDetail)
        model.addAttribute("commentsOfLaptop", commentsOfLaptop)
        model.addAttribute("commentRequest", CommentRequest())
        return "laptop-detail"
    }

    // 코멘트 등록
    @PostMapping("/comments")
    fun addComment(@ModelAttribute commentRequest: CommentRequest): String {
        commentController.saveComment(commentRequest)
        return "redirect:/laptops/${commentRequest.laptopId}"
    }

    // 코멘트 수정
    @PostMapping("/comments/{commentId}/edit")
    fun editComment(
        @PathVariable commentId: Long,
        @ModelAttribute commentRequest: CommentRequest,
    ): String {
        val apiUrl = "$apiBaseUrl/api/comments/$commentId/edit"
        restTemplate.put(apiUrl, commentRequest)
        return "redirect:/laptops/${commentRequest.laptopId}"
    }


}