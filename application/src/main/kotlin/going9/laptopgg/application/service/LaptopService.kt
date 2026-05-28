package going9.laptopgg.application.service

import going9.laptopgg.application.laptop.LaptopDetailResult
import going9.laptopgg.application.port.out.LaptopPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopService(
    private val laptopPort: LaptopPort
) {

    @Transactional(readOnly = true)
    fun findLaptopById(laptopId: Long): LaptopDetailResult {
        val laptop = laptopPort.findWithUsageById(laptopId) ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        return LaptopDetailResult.from(laptop)
    }
}
