package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.response.CommentResponse
import going9.laptopgg.web.dto.response.LaptopDetailResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
internal class LaptopPageController(
    private val getLaptopDetailUseCase: GetLaptopDetailUseCase,
    private val manageCommentUseCase: ManageCommentUseCase,
) {
    @GetMapping("/laptops/{laptopId}")
    fun showLaptopDetail(@PathVariable laptopId: Long, model: Model): String {
        val laptopDetail = LaptopDetailResponse.from(getLaptopDetailUseCase.get(laptopId))
        val commentsOfLaptop = manageCommentUseCase.listByLaptop(laptopId).map(CommentResponse::from)
        model.addAttribute("laptopDetail", laptopDetail)
        model.addAttribute("commentsOfLaptop", commentsOfLaptop)
        model.addAttribute("commentRequest", CommentRequest(laptopId = laptopId))
        return "laptop-detail"
    }
}
