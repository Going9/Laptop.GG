package going9.laptopgg.service

import going9.laptopgg.domain.repository.NewLaptopRepository
import going9.laptopgg.dto.response.LaptopDetailResponse
import going9.laptopgg.util.findByOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopServiceTMP(
    private val newLaptopRepository: NewLaptopRepository
) {

    @Transactional
    fun findLaptopById(laptopId: Long): LaptopDetailResponse{
        val laptop = newLaptopRepository.findByOrThrow(laptopId)
        return LaptopDetailResponse.of(laptop)
    }
}