package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopScreen(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    @ManyToOne
    @JoinColumn(name = "screen_id")
    val screen: Screen,

    val resolution: String,  // 1920*1080 로 넣고 타입캐스팅 하여 곱해서 연산

    val brightness: Int,

    val colorGamut: ColorGamut,

    val refreshRate: Int,

    val glare: GlareType,

    val screenSize: Double,

    val screenTouch: Boolean,

    val isSixTeenNine: Boolean,

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