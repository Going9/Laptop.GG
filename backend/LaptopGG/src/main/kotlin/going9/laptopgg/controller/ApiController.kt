package going9.laptopgg.controller

import going9.laptopgg.service.LaptopService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiController(
    private val laptopService: LaptopService,
) {
}