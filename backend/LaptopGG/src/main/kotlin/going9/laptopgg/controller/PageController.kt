package going9.laptopgg.controller

import going9.laptopgg.domain.laptop.*
import going9.laptopgg.dto.request.CpuRequest
import going9.laptopgg.dto.request.GpuRequest
import going9.laptopgg.dto.request.LaptopRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class PageController(
    private val cpuController: CpuController,
    private val gpuController: GpuController,
    private val laptopController: LaptopController,
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

    // 노트북 등록 페이지
    @GetMapping("/laptops/new")
    fun showLaptopForm(model: Model): String {
        model.addAttribute("laptopRequest", LaptopRequest())
        model.addAttribute("laptopCategory", LaptopCategory.entries)
        model.addAttribute("cpus", cpuController.getAllCpus())
        model.addAttribute("gpus", gpuController.getAllGpus())
        model.addAttribute("panelTypes", PanelType.entries)
        model.addAttribute("colorAccuracyTypes", ColorAccuracy.entries)
        model.addAttribute("glareTypes", GlareType.entries)
        return "laptop-form"
    }

    // 노트북 생성 로직
    @PostMapping("/laptops")
    fun saveLaptop(@ModelAttribute laptopRequest: LaptopRequest): String {
        laptopController.saveLaptop(laptopRequest)
        return "redirect:/laptops/new"
    }

    // cpu 조회 페이지
    @GetMapping("/cpus")
    fun getAllCpus(model: Model): String {
        val cpus = cpuController.getAllCpus()
        model.addAttribute("cpus", cpus)
        return "cpu-list"
    }

    // cpu 등록 로직
    @PostMapping("/cpus")
    fun saveCpu(@ModelAttribute cpuRequest: CpuRequest): String {
        cpuController.saveCpu(cpuRequest)
        return "redirect:/cpus"
    }

    // cpu 등록 페이지
    @GetMapping("/cpus/new")
    fun showCpuForm(model: Model): String {
        model.addAttribute("cpuRequest", CpuRequest.default())
        model.addAttribute("manufacturers", CpuManufacturer.entries)
        return "cpu-form"
    }

    // gpu 등록 로직
    @PostMapping("/gpus")
    fun saveGpu(@ModelAttribute gpuRequest: GpuRequest): String {
        gpuController.saveGpu(gpuRequest)
        return "redirect:/gpus"
    }

    // gpu 등록 페이지
    @GetMapping("/gpus/new")
    fun showGpuForm(model: Model): String {
        model.addAttribute("gpuRequest", GpuRequest.default())
        model.addAttribute("manufacturers", GpuManufacturer.entries)
        return "gpu-form"
    }

    // gpu 확인
    @GetMapping("/gpus")
    fun getAllGpus(model: Model): String {
        val gpus = gpuController.getAllGpus()
        model.addAttribute("gpus", gpus)
        return "gpu-list"
    }


}