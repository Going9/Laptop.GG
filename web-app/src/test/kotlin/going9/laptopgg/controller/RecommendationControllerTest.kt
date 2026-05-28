package going9.laptopgg.controller

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageRequest

class RecommendationControllerTest {
    private val recommendLaptopsUseCase = Mockito.mock(RecommendLaptopsUseCase::class.java)
    private val controller = RecommendationController(recommendLaptopsUseCase)

    @Test
    fun `recommend api returns explicit paged response schema`() {
        val request = LaptopRecommendationRequest.fixture()
        val pageable = PageRequest.of(0, 10)
        val laptop = LaptopRecommendationListResponse(
            id = 1L,
            score = 91.2,
            imgLink = "https://example.com/laptop.jpg",
            price = 1_490_000,
            name = "테스트 노트북",
            manufacturer = "테스트",
            weight = 1.2,
            screenSize = 14,
            cpu = "Core Ultra",
            gpu = "Intel Graphics",
            resolutionLabel = "FHD",
            reasons = listOf("문서 작업에 잘 맞아요"),
        )

        Mockito.`when`(recommendLaptopsUseCase.recommend(request, pageable.toPageQuery()))
            .thenReturn(
                PagedResult(
                    content = listOf(laptop),
                    page = 0,
                    size = 10,
                    totalElements = 11,
                    sort = pageable.toPageQuery().sort,
                ),
            )

        val response = controller.recommendLaptops(request, pageable)

        assertThat(response.content).containsExactly(laptop)
        assertThat(response.page).isEqualTo(0)
        assertThat(response.size).isEqualTo(10)
        assertThat(response.totalElements).isEqualTo(11)
        assertThat(response.totalPages).isEqualTo(2)
        assertThat(response.hasNext).isTrue()
        assertThat(response.hasPrevious).isFalse()
        assertThat(response.sort).isEmpty()
    }
}
