package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.Ram
import going9.laptopgg.domain.repository.RamRepository
import going9.laptopgg.dto.request.RamRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RamService(
    val ramRepository: RamRepository,
) {

    @Transactional
    fun saveRam(laptop: Laptop, request: RamRequest) {
        val ram = Ram(
            laptop,
            request.capacity,
            request.slot,
            request.clockSpeed,
            request.ddrType
        )
        ramRepository.save(ram)
    }
}