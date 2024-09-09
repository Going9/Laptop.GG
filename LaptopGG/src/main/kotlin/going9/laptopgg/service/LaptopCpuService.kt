package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopCpu
import going9.laptopgg.domain.repository.LaptopCpuRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopCpuService(
    private val laptopCpuRepository: LaptopCpuRepository
) {

    @Transactional
    fun saveLaptopCpu(laptop: Laptop, cpu: Cpu) {
        val laptopCpu = LaptopCpu(
            laptop,
            cpu
        )
        laptopCpuRepository.save(laptopCpu)
    }
}