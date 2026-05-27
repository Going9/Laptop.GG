package going9.laptopgg.application.laptop

import going9.laptopgg.dto.response.LaptopDetailResponse
import going9.laptopgg.service.LaptopService
import org.springframework.stereotype.Service

@Service
class GetLaptopDetailUseCase(
    private val laptopService: LaptopService,
) {
    fun get(laptopId: Long): LaptopDetailResponse {
        return laptopService.findLaptopById(laptopId)
    }
}
