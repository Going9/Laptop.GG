package going9.laptopgg.service

import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationService(

) {

    @Transactional
    fun recommendLaptop(request: LaptopRecommendationRequest): List<LaptopRecommendationResponse> {


        return emptyList()
    }
}