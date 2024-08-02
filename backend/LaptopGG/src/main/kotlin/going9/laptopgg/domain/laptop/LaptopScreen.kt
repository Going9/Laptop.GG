package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopScreen(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val panel: PanelType,

    val resolution: String,  // 1920*1080 로 넣고 타입캐스팅 하여 곱해서 연산

    val brightness: Int,

    val colorGamut: ColorGamut,

    val refreshRate: Int,

    val isGlare: GlareType,

    val screenSize: Double,  // 15.6, 16, ...

    val isTouch: Boolean,

    val isSixteenNine: Boolean,

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

enum class ColorGamut {
    BAD,
    GOOD,
    DESIGNER,
}

enum class PanelType {
    IPS,
    OLED
}