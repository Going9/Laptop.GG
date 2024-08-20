package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Gpu
import going9.laptopgg.domain.repository.GpuRepository
import going9.laptopgg.dto.request.GpuRequest
import going9.laptopgg.dto.response.GpuResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GpuService(
    private val gpuRepository: GpuRepository,
) {
    @Transactional
    fun saveGpu(request: GpuRequest) {
        val gpu = Gpu(
            mutableListOf(),
            request.name,
            request.manufacturer,
            request.isIgpu,
        )
        gpuRepository.save(gpu)
    }

    @Transactional(readOnly = true)
    fun getAllGpus(): List<GpuResponse> {
        return gpuRepository.findAll()
            .map { gpu -> GpuResponse.of(gpu) }
    }

    @Transactional(readOnly = true)
    fun findByIds(ids: List<Long>): List<Gpu> {
        return gpuRepository.findAllById(ids)
    }
}