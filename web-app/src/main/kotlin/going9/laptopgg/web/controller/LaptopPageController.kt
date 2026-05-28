package going9.laptopgg.web.controller

import going9.laptopgg.application.laptop.GetLaptopDetailPageUseCase
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.response.CommentResponse
import going9.laptopgg.web.dto.response.LaptopDetailResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
internal class LaptopPageController(
    private val getLaptopDetailPageUseCase: GetLaptopDetailPageUseCase,
) {
    @GetMapping("/laptops/{laptopId}")
    fun showLaptopDetail(@PathVariable laptopId: Long, model: Model): String {
        val pageResult = getLaptopDetailPageUseCase.get(laptopId)
        val laptopDetail = LaptopDetailResponse.from(pageResult.laptopDetail)
        val commentsOfLaptop = pageResult.comments.map(CommentResponse::from)
        model.addAttribute("laptopDetail", laptopDetail)
        model.addAttribute("commentsOfLaptop", commentsOfLaptop)
        model.addAttribute("commentRequest", CommentRequest(laptopId = laptopId))
        return "laptop-detail"
    }
}
