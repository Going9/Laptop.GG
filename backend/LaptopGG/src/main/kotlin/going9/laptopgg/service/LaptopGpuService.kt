package going9.laptopgg.service

import going9.laptopgg.domain.laptop.LaptopGpu
import going9.laptopgg.domain.repository.LaptopGpuRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopGpuService(
    private val laptopGpuRepository : LaptopGpuRepository,
) {

    @Transactional
    fun saveLaptopGpu(laptopGpu: LaptopGpu) {
        laptopGpuRepository.save(laptopGpu)
    }
}