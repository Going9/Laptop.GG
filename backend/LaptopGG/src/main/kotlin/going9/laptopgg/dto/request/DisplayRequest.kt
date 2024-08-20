package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.ColorAccuracy
import going9.laptopgg.domain.laptop.GlareType
import going9.laptopgg.domain.laptop.PanelType

class DisplayRequest(
    val panel: PanelType,  // 패널 타입 (IPS, OLED 등)
    val resolutionWidth: Int,  // 가로 해상도
    val resolutionHeight: Int, // 세로 해상도
    val brightness: Int,  // 밝기 (니트)
    val colorAccuracy: ColorAccuracy,  // 색 정확도
    val refreshRate: Int,  // 주사율 (Hz)
    val glareType: GlareType,  // 글레어 유형 (NONE, GLARE, ANTI_GLARE)
    val screenSize: Double,  // 화면 크기 (인치 단위, 예: 15.6, 16 등)
    val isTouch: Boolean,  // 터치 스크린 여부
    val aspectRatio: String,  // 화면 비율 (예: "16:9", "16:10")
) {
}