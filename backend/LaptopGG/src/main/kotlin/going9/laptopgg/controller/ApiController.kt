package going9.laptopgg.controller

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.dto.request.CpuRequest
import going9.laptopgg.service.CpuService
import going9.laptopgg.service.LaptopService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiController(
    private val laptopService: LaptopService,
    private val cpuService: CpuService,
) {

    @GetMapping
    fun getAllCpus(): ResponseEntity<List<Cpu>> {
        return ResponseEntity.ok(cpuService.getAllCpus())
    }

    @PostMapping
    fun saveCpu(@RequestBody request: CpuRequest) {
        cpuService.saveCpu(request)
    }
}