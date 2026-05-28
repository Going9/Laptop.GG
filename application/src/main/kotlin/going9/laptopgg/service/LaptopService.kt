package going9.laptopgg.service

import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.dto.response.LaptopDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopService(
    private val laptopPort: LaptopPort
) {

    @Transactional(readOnly = true)
    fun findLaptopById(laptopId: Long): LaptopDetailResponse{
        val laptop = laptopPort.findWithUsageById(laptopId) ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        return LaptopDetailResponse.of(laptop)
    }
}
