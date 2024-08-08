package going9.laptopgg.controller

import going9.laptopgg.domain.laptop.CpuManufacturer
import going9.laptopgg.dto.request.CpuRequest
import going9.laptopgg.service.CpuService
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class PageController(
    private val restTemplateBuilder: RestTemplateBuilder,
    private val cpuService: CpuService
) {

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

    @GetMapping("/cpus/new")
    fun showCpuForm(model: Model): String {
        model.addAttribute("cpuRequest", CpuRequest())
        model.addAttribute("manufacturers", CpuManufacturer.values())
        return "cpu-form"
    }

    @GetMapping("/cpus")
    fun getAllCpus(model: Model): String {
        val cpus = cpuService.getAllCpus()
        model.addAttribute("cpus", cpus)
        return "cpu-list"
    }

    @PostMapping("/cpus")
    fun saveCpu(@ModelAttribute cpuRequest: CpuRequest): String {
        cpuService.saveCpu(cpuRequest)
        return "redirect:/cpus"
    }
}