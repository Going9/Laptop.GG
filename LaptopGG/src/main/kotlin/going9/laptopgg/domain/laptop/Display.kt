package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Display(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val panel: PanelType,  // 패널 타입 (IPS, OLED 등)

    val resolutionWidth: Int,  // 가로 해상도

    val resolutionHeight: Int, // 세로 해상도

    val brightness: Int,  // 밝기 (니트)

    val colorAccuracy: ColorAccuracy,  // 색 정확도

    val refreshRate: Int,  // 주사율 (Hz)

    val glareType: GlareType,  // 글레어 유형 (NONE, GLARE, ANTI_GLARE)

    val screenSize: Int,  // 화면 크기 (인치 단위, 예: 15.6, 16 등)

    val isTouch: Boolean,  // 터치 스크린 여부

    val aspectRatio: String,  // 화면 비율 (예: "16:9", "16:10")

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}

enum class GlareType {
    NONE,
    GLARE,
    ANTI_GLARE
}

enum class ColorAccuracy {
    BAD,
    GOOD,
    DESIGNER,
}

enum class PanelType {
    IPS,
    OLED
}
