package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.ColorAccuracy
import going9.laptopgg.domain.laptop.GlareType
import going9.laptopgg.domain.laptop.PanelType

data class DisplayRequest(
    val panel: PanelType = PanelType.IPS,  // 패널 타입 (IPS, OLED 등)
    val resolutionWidth: Int = 0,  // 가로 해상도
    val resolutionHeight: Int = 0, // 세로 해상도
    val brightness: Int = 0,  // 밝기 (니트)
    val colorAccuracy: ColorAccuracy = ColorAccuracy.GOOD,  // 색 정확도
    val refreshRate: Int = 0,  // 주사율 (Hz)
    val glareType: GlareType = GlareType.GLARE,  // 글레어 유형 (NONE, GLARE, ANTI_GLARE)
    val screenSize: Double = 0.0,  // 화면 크기 (인치 단위, 예: 15.6, 16 등)
    val isTouch: Boolean = false,  // 터치 스크린 여부
    val aspectRatio: String = "",  // 화면 비율 (예: "16:9", "16:10")
) {
}