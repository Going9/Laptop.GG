package going9.laptopgg.controller

import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.dto.response.LaptopDetailResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/laptops")
class LaptopController(
    private val getLaptopDetailUseCase: GetLaptopDetailUseCase,
) {

    @GetMapping
    fun getLaptopDetail(@RequestParam id: Long): LaptopDetailResponse {
        return LaptopDetailResponse.from(getLaptopDetailUseCase.get(id))
    }

}
