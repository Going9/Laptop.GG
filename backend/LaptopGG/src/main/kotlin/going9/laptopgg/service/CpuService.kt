package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.domain.repository.CpuRepository
import going9.laptopgg.dto.request.CpuRequest
import going9.laptopgg.dto.response.CpuResponse
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
            request.manufacturer,
            request.isHighPower,
        )
        cpuRepository.save(cpu)
    }

    @Transactional(readOnly = true)
    fun getAllCpus(): List<CpuResponse> {
        return cpuRepository.findAll()
            .map { cpu -> CpuResponse.of(cpu)}
    }

    @Transactional(readOnly = true)
    fun findByIds(ids: List<Long>): List<Cpu> {
        return cpuRepository.findAllById(ids)
    }
}