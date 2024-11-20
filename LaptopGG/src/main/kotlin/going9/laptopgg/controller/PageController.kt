package going9.laptopgg.controller

import going9.laptopgg.domain.laptop.*
import going9.laptopgg.dto.request.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.client.RestTemplate

@Controller
class PageController(
    @Value("\${spring.api.base-url:http://localhost:8080}")
    private val apiBaseUrl: String,

    private val restTemplate: RestTemplate,
    private val cpuController: CpuController,
    private val gpuController: GpuController,
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

    @PostMapping("/recommends")
    fun recommendLaptop(@ModelAttribute laptopRecommendationRequest: LaptopRecommendationRequest, model: Model): String {
        val recommendedLaptops = recommendationController.recommendLaptop(laptopRecommendationRequest)
        model.addAttribute("recommendedLaptops", recommendedLaptops)
        return "recommendation-list"
    }

    @GetMapping("/laptops/{laptopId}")
    fun showLaptopDetailTMP(@PathVariable laptopId: Long, model: Model): String {
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