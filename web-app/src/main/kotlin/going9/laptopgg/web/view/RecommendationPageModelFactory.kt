package going9.laptopgg.web.view

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.dto.response.LaptopRecommendationListResponse
import org.springframework.stereotype.Component

@Component
class RecommendationPageModelFactory(
    private val useCasePresentation: RecommendationUseCasePresentation,
    private val screenSizePresentation: RecommendationScreenSizePresentation,
    private val presetCatalog: RecommendationPresetCatalog,
) {
    fun formAttributes(): Map<String, Any> {
        return mapOf(
            "laptopRecommendationRequest" to LaptopRecommendationRequest(),
            "screenSizeList" to LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES,
            "screenSizeModeOptions" to screenSizePresentation.options(),
            "useCaseOptions" to useCasePresentation.options(),
            "budgetPresetList" to presetCatalog.budgetPresets(),
            "weightPresetList" to presetCatalog.weightPresets(),
        )
    }

    fun resultAttributes(
        request: LaptopRecommendationRequest,
        recommendedLaptops: PagedResult<LaptopRecommendationResult>,
        pageQuery: PageQuery,
    ): Map<String, Any> {
        val mappedRecommendations = recommendedLaptops.map(LaptopRecommendationListResponse::from)
        val resolvedUseCase = request.resolvedUseCase()
        return mapOf(
            "laptopRecommendationRequest" to request,
            "recommendedLaptops" to mappedRecommendations.content,
            "totalCount" to mappedRecommendations.totalElements,
            "totalPages" to mappedRecommendations.totalPages,
            "currentSort" to (pageQuery.sort.firstOrNull()?.toQueryParameter() ?: "recommended"),
            "currentPage" to mappedRecommendations.page + 1,
            "totalPage" to mappedRecommendations.totalPages,
            "resolvedUseCase" to resolvedUseCase,
            "resolvedUseCaseLabel" to useCasePresentation.label(resolvedUseCase),
            "resolvedUseCaseHeading" to useCasePresentation.heading(resolvedUseCase),
            "screenSizeSummary" to screenSizePresentation.summary(request),
        )
    }
}
