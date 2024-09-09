package going9.laptopgg.controller

import going9.laptopgg.dto.request.GpuRequest
import going9.laptopgg.dto.response.GpuResponse
import going9.laptopgg.service.GpuService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/gpus")
class GpuController(
    private val gpuService: GpuService,
) {

    @PostMapping
    fun saveGpu(@RequestBody gpuRequest: GpuRequest) {
        gpuService.saveGpu(gpuRequest)
    }

    @GetMapping
    fun getAllGpus(): List<GpuResponse> {
        return gpuService.getAllGpus()
    }

}