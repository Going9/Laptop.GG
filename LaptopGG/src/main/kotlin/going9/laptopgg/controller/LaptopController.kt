package going9.laptopgg.controller

import going9.laptopgg.dto.request.LaptopRequest
import going9.laptopgg.dto.response.LaptopRecommendationDetailResponse
import going9.laptopgg.service.LaptopService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/laptops")
class LaptopController(
    private val laptopService: LaptopService,
) {

    @PostMapping
    fun saveLaptop(@RequestBody laptopRequest: LaptopRequest) {
        laptopService.saveLaptop(laptopRequest)
    }

    @GetMapping
    fun getLaptop(@RequestParam id: Long): LaptopRecommendationDetailResponse {
        return laptopService.findLaptopById(id)
    }

}