package going9.laptopgg.web.view

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.recommendation.RecommendationUseCase
import going9.laptopgg.web.controller.pageQueryFrom
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationPageModelFactoryTest {
    private val useCasePresentation = RecommendationUseCasePresentation()
    private val screenSizePresentation = RecommendationScreenSizePresentation()
    private val factory = RecommendationPageModelFactory(
        useCasePresentation = useCasePresentation,
        screenSizePresentation = screenSizePresentation,
        presetCatalog = RecommendationPresetCatalog(),
    )

    @Test
    fun `form page contains selectable recommendation options`() {
        val page = factory.formPage()

        assertThat(page.laptopRecommendationRequest).isInstanceOf(LaptopRecommendationRequest::class.java)
        assertThat(page.screenSizeList).isEqualTo(LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES)
        assertThat(page.screenSizeModeOptions).isNotEmpty()
        assertThat(page.useCaseOptions).isNotEmpty()
        assertThat(page.budgetPresetList).isNotEmpty()
        assertThat(page.weightPresetList).isNotEmpty()
    }

    @Test
    fun `result page maps page metadata and presentation labels`() {
        val request = LaptopRecommendationRequest()
        val pageQuery = pageQueryFrom(page = 0, size = 10, sort = listOf("price,asc"))
        val recommendations = PagedResult(
            content = listOf(recommendationResult()),
            page = 0,
            size = 10,
            totalElements = 1,
        )

        val page = factory.resultPage(request, recommendations, pageQuery)

        assertThat(page.totalCount).isEqualTo(1L)
        assertThat(page.currentSort).isEqualTo("price,asc")
        assertThat(page.currentPage).isEqualTo(1)
        assertThat(page.resolvedUseCaseLabel).isEqualTo("두루 쓰기 좋은")
        assertThat(page.resolvedUseCaseHeading).isEqualTo("두루 쓰기 좋은 후보")
        assertThat(page.screenSizeSummary).isEqualTo("화면 크기 상관없음")
    }

    @Test
    fun `presentation catalog keeps use case labels and screen summaries`() {
        val request = LaptopRecommendationRequest(
            screenSizeMode = ScreenSizeMode.SELECT,
            screenSizes = listOf(16, 14),
        )

        assertThat(useCasePresentation.label(RecommendationUseCase.CREATOR))
            .isEqualTo("사진·영상 작업")
        assertThat(useCasePresentation.heading(RecommendationUseCase.CREATOR))
            .isEqualTo("사진·영상 작업에 맞는 후보")
        assertThat(screenSizePresentation.summary(request)).isEqualTo("화면 14형 · 16형")
    }

    private fun recommendationResult(): LaptopRecommendationResult {
        return LaptopRecommendationResult(
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
    }
}
