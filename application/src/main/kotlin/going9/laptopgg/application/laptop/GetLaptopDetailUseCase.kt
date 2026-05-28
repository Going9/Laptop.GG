package going9.laptopgg.application.laptop

import going9.laptopgg.application.service.LaptopService
import org.springframework.stereotype.Service

@Service
class GetLaptopDetailUseCase(
    private val laptopService: LaptopService,
) {
    fun get(laptopId: Long): LaptopDetailResult {
        return laptopService.findLaptopById(laptopId)
    }
}
