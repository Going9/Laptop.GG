package going9.laptopgg.service

import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.response.LaptopDetailResponse
import going9.laptopgg.util.findByOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopService(
    private val laptopRepository: LaptopRepository
) {

    @Transactional
    fun findLaptopById(laptopId: Long): LaptopDetailResponse{
        val laptop = laptopRepository.findByOrThrow(laptopId)
        return LaptopDetailResponse.of(laptop)
    }
}