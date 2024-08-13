package going9.laptopgg.controller

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.dto.request.CpuRequest
import going9.laptopgg.dto.request.GpuRequest
import going9.laptopgg.dto.response.CpuResponse
import going9.laptopgg.dto.response.GpuResponse
import going9.laptopgg.service.CpuService
import going9.laptopgg.service.GpuService
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
    private val gpuService: GpuService,
) {

    // Cpu
    @GetMapping("/cpus")
    fun getAllCpus(): List<CpuResponse> {
        return cpuService.getAllCpus()
    }

    @PostMapping("/cpus")
    fun saveCpu(@RequestBody cpuRequest: CpuRequest) {
        cpuService.saveCpu(cpuRequest)
    }

    // Gpu
    @PostMapping("/gpus")
    fun saveGpu(@RequestBody gpuRequest: GpuRequest) {
        gpuService.saveGpu(gpuRequest)
    }

    @GetMapping("/gpus")
    fun getAllGpus(): List<GpuResponse> {
        return gpuService.getAllGpus()
    }
}