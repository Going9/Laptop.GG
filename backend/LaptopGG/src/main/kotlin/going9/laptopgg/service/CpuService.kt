package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.domain.repository.CpuRepository
import going9.laptopgg.dto.request.CpuRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CpuService(
    private val cpuRepository: CpuRepository,
) {
    @Transactional
    fun saveCpu(request: CpuRequest) {
        val cpu = Cpu(
            mutableListOf(),
            request.name,
            request.isHighPower,
            request.manufacturer
        )
        cpuRepository.save(cpu)
    }

    fun getAllCpus(): List<Cpu> {
        return cpuRepository.findAll()
    }
}