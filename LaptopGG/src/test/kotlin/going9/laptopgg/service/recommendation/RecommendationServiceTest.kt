package going9.laptopgg.service.recommendation

import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.service.RecommendationService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceTest @Autowired constructor(
    private val laptopRepository: LaptopRepository,
    private val recommendationService: RecommendationService,
) {

    @Test
    @DisplayName("추천 로직이 정상 작동한다.")
    fun recommendLaptopTest() {
        // given
        val request: LaptopRecommendationRequest = LaptopRecommendationRequest.fixture()

        //when
        val result = recommendationService.recommendLaptop(request)

        //then
        result.toString()
        assertThat(result[0].manufacturer).isEqualTo("레노버")
    }

    @Test
    @DisplayName("노트북 전체 검색")
    fun findAllLaptops() {
        // given
        val result = laptopRepository.findAll()

        assertThat(result[0].manufacturer).isEqualTo("레노버")
    }
}