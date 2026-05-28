package going9.laptopgg.web.controller

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.web.view.RecommendationPageModelFactory
import going9.laptopgg.web.view.RecommendationPresetCatalog
import going9.laptopgg.web.view.RecommendationScreenSizePresentation
import going9.laptopgg.web.view.RecommendationUseCasePresentation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.ui.ExtendedModelMap

class RecommendationPageControllerTest {
    private val recommendLaptopsUseCase = Mockito.mock(RecommendLaptopsUseCase::class.java)
    private val controller = RecommendationPageController(
        recommendLaptopsUseCase = recommendLaptopsUseCase,
        recommendationPageModelFactory = RecommendationPageModelFactory(
            useCasePresentation = RecommendationUseCasePresentation(),
            screenSizePresentation = RecommendationScreenSizePresentation(),
            presetCatalog = RecommendationPresetCatalog(),
        ),
    )

    @Test
    fun `recommendation form keeps expected model attributes`() {
        val model = ExtendedModelMap()

        val viewName = controller.showRecommendationForm(model)

        assertThat(viewName).isEqualTo("recommendation-form")
        assertThat(model["laptopRecommendationRequest"]).isInstanceOf(LaptopRecommendationRequest::class.java)
        assertThat(model["screenSizeList"]).isEqualTo(LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES)
        assertThat(model["budgetPresetList"]).isNotNull
        assertThat(model["weightPresetList"]).isNotNull
    }

    @Test
    fun `recommendation result delegates to service and keeps list model attributes`() {
        val request = LaptopRecommendationRequest()
        val query = request.toQuery()
        val pageQuery = pageQueryFrom(page = 0, size = 10, sort = listOf("price,asc"))
        val recommendedLaptop = LaptopRecommendationResult(
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
        Mockito.`when`(recommendLaptopsUseCase.recommend(query, pageQuery))
            .thenReturn(
                PagedResult(
                    content = listOf(recommendedLaptop),
                    page = 0,
                    size = 10,
                    totalElements = 1,
                ),
            )
        val model = ExtendedModelMap()

        val viewName = controller.recommendLaptops(
            laptopRecommendationRequest = request,
            page = 0,
            size = 10,
            sort = listOf("price,asc"),
            model = model,
        )

        assertThat(viewName).isEqualTo("recommendation-list")
        assertThat(model["recommendedLaptops"]).isEqualTo(listOf(LaptopRecommendationListResponse.from(recommendedLaptop)))
        assertThat(model["totalCount"]).isEqualTo(1L)
        assertThat(model["currentSort"]).isEqualTo("price,asc")
    }
}
