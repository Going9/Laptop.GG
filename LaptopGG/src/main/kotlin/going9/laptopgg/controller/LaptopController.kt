package going9.laptopgg.controller

import going9.laptopgg.dto.request.LaptopRequest
import going9.laptopgg.service.LaptopService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LaptopController(
    private val laptopService: LaptopService,
) {
}