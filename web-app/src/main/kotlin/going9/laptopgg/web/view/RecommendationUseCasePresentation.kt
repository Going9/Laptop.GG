package going9.laptopgg.web.view

import going9.laptopgg.recommendation.RecommendationUseCase
import org.springframework.stereotype.Component

@Component
internal class RecommendationUseCasePresentation {
    fun options(): List<UseCaseOption> {
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

    fun label(useCase: RecommendationUseCase): String {
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

    fun heading(useCase: RecommendationUseCase): String {
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> "두루 쓰기 좋은 후보"
            else -> "${label(useCase)}에 맞는 후보"
        }
    }
}

internal data class UseCaseOption(
    val value: RecommendationUseCase,
    val label: String,
    val description: String,
)
