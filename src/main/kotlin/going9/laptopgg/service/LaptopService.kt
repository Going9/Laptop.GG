package going9.laptopgg.service

import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.response.LaptopDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopService(
    private val laptopRepository: LaptopRepository
) {

    @Transactional(readOnly = true)
    fun findLaptopById(laptopId: Long): LaptopDetailResponse{
        val laptop = laptopRepository.findWithUsageById(laptopId) ?: throw IllegalArgumentException()
        return LaptopDetailResponse.of(laptop)
    }
}
