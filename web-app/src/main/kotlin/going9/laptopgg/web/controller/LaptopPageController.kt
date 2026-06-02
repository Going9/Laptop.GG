package going9.laptopgg.web.controller

import going9.laptopgg.application.laptop.GetLaptopDetailPageUseCase
import going9.laptopgg.web.view.LaptopDetailPageModelFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
internal class LaptopPageController(
    private val getLaptopDetailPageUseCase: GetLaptopDetailPageUseCase,
    private val laptopDetailPageModelFactory: LaptopDetailPageModelFactory,
) {
    @GetMapping("/laptops/{laptopId}")
    fun showLaptopDetail(@PathVariable laptopId: Long, model: Model): String {
        val pageResult = getLaptopDetailPageUseCase.get(laptopId)
        model.addAttribute("laptopPage", laptopDetailPageModelFactory.create(laptopId, pageResult))
        return "laptop-detail"
    }
}
