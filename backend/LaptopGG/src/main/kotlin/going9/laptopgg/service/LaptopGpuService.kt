package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Gpu
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopGpu
import going9.laptopgg.domain.repository.LaptopGpuRepository
import going9.laptopgg.dto.request.LaptopGpuRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopGpuService(
    private val laptopGpuRepository : LaptopGpuRepository,
) {

    @Transactional
    fun saveLaptopGpu(laptop: Laptop, gpu: Gpu, request: LaptopGpuRequest) {
        val laptopGpu = LaptopGpu(
            laptop,
            gpu,
            request.tgp,
            request.isMux
        )
        laptopGpuRepository.save(laptopGpu)
    }
}