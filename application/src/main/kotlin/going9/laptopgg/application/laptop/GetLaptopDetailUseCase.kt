package going9.laptopgg.application.laptop

import going9.laptopgg.application.port.out.LaptopPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetLaptopDetailUseCase(
    private val laptopPort: LaptopPort,
) {
    @Transactional(readOnly = true)
    fun get(laptopId: Long): LaptopDetailResult {
        val laptop = laptopPort.findWithUsageById(laptopId) ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        return LaptopDetailResult.from(laptop)
    }
}
