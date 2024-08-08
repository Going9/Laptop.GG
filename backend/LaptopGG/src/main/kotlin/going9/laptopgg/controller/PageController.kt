package going9.laptopgg.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class PageController {

    @GetMapping("/")
    fun index(): String {
        return "index"
    }

    @PostMapping("/spec-form")
    fun specForm(
        @RequestParam budget: Int,
        @RequestParam usage: String,
        @RequestParam(required = false) gameType: String?,
        @RequestParam weight: Float,
        @RequestParam tenkey: String,
        @RequestParam foreignLaptop: String,
        model: Model
    ): String {
        model.addAttribute("budget", budget)
        model.addAttribute("usage", usage)
        model.addAttribute("gameType", gameType)
        model.addAttribute("weight", weight)
        model.addAttribute("tenkey", tenkey)
        model.addAttribute("foreignLaptop", foreignLaptop)
        return "spec-form"
    }
}