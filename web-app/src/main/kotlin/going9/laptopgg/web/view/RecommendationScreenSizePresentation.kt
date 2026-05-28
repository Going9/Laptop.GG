package going9.laptopgg.web.view

import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.web.dto.request.LaptopRecommendationRequest
import org.springframework.stereotype.Component

@Component
internal class RecommendationScreenSizePresentation {
    fun options(): List<ScreenSizeModeOption> {
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

    fun summary(request: LaptopRecommendationRequest): String {
        return when (request.resolvedScreenSizeMode()) {
            ScreenSizeMode.ANY -> "화면 크기 상관없음"
            ScreenSizeMode.NOT_SURE -> "화면 크기 잘 모르겠어요"
            ScreenSizeMode.SELECT -> "화면 ${request.normalizedScreenSizes().joinToString(" · ") { "${it}형" }}"
        }
    }
}

internal data class ScreenSizeModeOption(
    val value: ScreenSizeMode,
    val label: String,
    val description: String,
)
