package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Ram
import going9.laptopgg.domain.repository.RamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RamService(
    val ramRepository: RamRepository,
) {

    @Transactional
    fun saveRam(ram: Ram) {
        ramRepository.save(ram)
    }
}