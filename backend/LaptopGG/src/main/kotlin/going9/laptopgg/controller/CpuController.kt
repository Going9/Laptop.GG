package going9.laptopgg.controller

import going9.laptopgg.dto.request.CpuRequest
import going9.laptopgg.dto.response.CpuResponse
import going9.laptopgg.service.CpuService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cpus")
class CpuController(
    private val cpuService: CpuService
) {

    // Cpu
    @GetMapping()
    fun getAllCpus(): List<CpuResponse> {
        return cpuService.getAllCpus()
    }

    @PostMapping()
    fun saveCpu(@RequestBody cpuRequest: CpuRequest) {
        cpuService.saveCpu(cpuRequest)
    }
}