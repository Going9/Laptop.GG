package going9.laptopgg.web.view

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.recommendation.RecommendationUseCase
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import going9.laptopgg.web.dto.response.LaptopRecommendationListResponse
import org.springframework.stereotype.Component

@Component
class RecommendationPageModelFactory {
    fun formAttributes(): Map<String, Any> {
        return mapOf(
            "laptopRecommendationRequest" to LaptopRecommendationRequest.fixture(),
            "screenSizeList" to LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES,
            "screenSizeModeOptions" to screenSizeModeOptions(),
            "useCaseOptions" to useCaseOptions(),
            "budgetPresetList" to budgetPresetList(),
            "weightPresetList" to weightPresetList(),
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
            "resolvedUseCaseLabel" to useCaseLabel(resolvedUseCase),
            "resolvedUseCaseHeading" to useCaseHeading(resolvedUseCase),
            "screenSizeSummary" to screenSizeSummary(request),
        )
    }

    private fun useCaseOptions(): List<UseCaseOption> {
        return listOf(
            UseCaseOption(
                value = RecommendationUseCase.NOT_SURE,
                label = "잘 모르겠어요",
                description = "문서 작업, 배터리, 휴대성의 균형을 먼저 봅니다.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.OFFICE_STUDY,
                label = "문서·학습",
                description = "강의, 문서 작업, 웹서핑이 많아요.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.PORTABLE_OFFICE,
                label = "휴대성 우선",
                description = "가볍고 들고 다니기 쉬운 모델이 좋아요.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.BATTERY_FIRST,
                label = "배터리 우선",
                description = "충전 없이 오래 쓰는 게 가장 중요해요.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.CASUAL_GAME,
                label = "가벼운 게임",
                description = "롤, 메이플 같은 가벼운 게임을 즐겨요.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.ONLINE_GAME,
                label = "온라인 게임",
                description = "발로란트, 로아 같은 게임을 자주 해요.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.AAA_GAME,
                label = "고사양 게임",
                description = "스팀 대작이나 고사양 게임이 중요해요.",
            ),
            UseCaseOption(
                value = RecommendationUseCase.CREATOR,
                label = "사진·영상 작업",
                description = "편집, 디자인, 제작 작업이 많아요.",
            ),
        )
    }

    private fun screenSizeModeOptions(): List<ScreenSizeModeOption> {
        return listOf(
            ScreenSizeModeOption(
                value = ScreenSizeMode.ANY,
                label = "상관없음",
                description = "화면 크기 조건 없이 넓게 추천받아요.",
            ),
            ScreenSizeModeOption(
                value = ScreenSizeMode.SELECT,
                label = "직접 고를게요",
                description = "원하는 크기를 여러 개 함께 선택할 수 있어요.",
            ),
            ScreenSizeModeOption(
                value = ScreenSizeMode.NOT_SURE,
                label = "잘 모르겠어요",
                description = "보통 많이 찾는 크기 중심으로 추천받아요.",
            ),
        )
    }

    private fun screenSizeSummary(request: LaptopRecommendationRequest): String {
        return when (request.resolvedScreenSizeMode()) {
            ScreenSizeMode.ANY -> "화면 크기 상관없음"
            ScreenSizeMode.NOT_SURE -> "화면 크기 잘 모르겠어요"
            ScreenSizeMode.SELECT -> "화면 ${request.normalizedScreenSizes().joinToString(" · ") { "${it}형" }}"
        }
    }

    private fun useCaseLabel(useCase: RecommendationUseCase): String {
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> "두루 쓰기 좋은"
            RecommendationUseCase.OFFICE_STUDY -> "문서·학습"
            RecommendationUseCase.PORTABLE_OFFICE -> "휴대성 우선"
            RecommendationUseCase.BATTERY_FIRST -> "배터리 우선"
            RecommendationUseCase.CASUAL_GAME -> "가벼운 게임"
            RecommendationUseCase.ONLINE_GAME -> "온라인 게임"
            RecommendationUseCase.AAA_GAME -> "고사양 게임"
            RecommendationUseCase.CREATOR -> "사진·영상 작업"
        }
    }

    private fun useCaseHeading(useCase: RecommendationUseCase): String {
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> "두루 쓰기 좋은 후보"
            else -> "${useCaseLabel(useCase)}에 맞는 후보"
        }
    }

    private fun budgetPresetList(): List<Int> {
        return (500_000..5_000_000 step 500_000).toList()
    }

    private fun weightPresetList(): List<Double> {
        return (1..8).map { it * 0.5 }
    }
}

data class UseCaseOption(
    val value: RecommendationUseCase,
    val label: String,
    val description: String,
)

data class ScreenSizeModeOption(
    val value: ScreenSizeMode,
    val label: String,
    val description: String,
)
