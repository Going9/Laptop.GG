package going9.laptopgg.web.view

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import going9.laptopgg.recommendation.RecommendationUseCase
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.dto.response.LaptopRecommendationListResponse
import org.springframework.stereotype.Component

@Component
internal class RecommendationPageModelFactory(
    private val useCasePresentation: RecommendationUseCasePresentation,
    private val screenSizePresentation: RecommendationScreenSizePresentation,
    private val presetCatalog: RecommendationPresetCatalog,
) {
    fun formPage(): RecommendationFormPageModel {
        return RecommendationFormPageModel(
            laptopRecommendationRequest = LaptopRecommendationRequest(),
            screenSizeList = LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES,
            screenSizeModeOptions = screenSizePresentation.options(),
            useCaseOptions = useCasePresentation.options(),
            budgetPresetList = presetCatalog.budgetPresets(),
            weightPresetList = presetCatalog.weightPresets(),
        )
    }

    fun resultPage(
        request: LaptopRecommendationRequest,
        recommendedLaptops: PagedResult<LaptopRecommendationResult>,
        pageQuery: PageQuery,
    ): RecommendationResultPageModel {
        val mappedRecommendations = recommendedLaptops.map(LaptopRecommendationListResponse::from)
        val resolvedUseCase = request.resolvedUseCase()
        return RecommendationResultPageModel(
            laptopRecommendationRequest = request,
            recommendedLaptops = mappedRecommendations.content,
            totalCount = mappedRecommendations.totalElements,
            totalPages = mappedRecommendations.totalPages,
            currentSort = pageQuery.sort.firstOrNull()?.toQueryParameter() ?: "recommended",
            currentPage = mappedRecommendations.page + 1,
            resolvedUseCase = resolvedUseCase,
            resolvedUseCaseLabel = useCasePresentation.label(resolvedUseCase),
            resolvedUseCaseHeading = useCasePresentation.heading(resolvedUseCase),
            screenSizeSummary = screenSizePresentation.summary(request),
        )
    }
}

internal data class RecommendationFormPageModel(
    val laptopRecommendationRequest: LaptopRecommendationRequest,
    val screenSizeList: List<Int>,
    val screenSizeModeOptions: List<ScreenSizeModeOption>,
    val useCaseOptions: List<UseCaseOption>,
    val budgetPresetList: List<Int>,
    val weightPresetList: List<Double>,
)

internal data class RecommendationResultPageModel(
    val laptopRecommendationRequest: LaptopRecommendationRequest,
    val recommendedLaptops: List<LaptopRecommendationListResponse>,
    val totalCount: Long,
    val totalPages: Int,
    val currentSort: String,
    val currentPage: Int,
    val resolvedUseCase: RecommendationUseCase,
    val resolvedUseCaseLabel: String,
    val resolvedUseCaseHeading: String,
    val screenSizeSummary: String,
)
