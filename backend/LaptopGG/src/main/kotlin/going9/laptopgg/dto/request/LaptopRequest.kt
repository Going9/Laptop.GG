package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.ColorAccuracy
import going9.laptopgg.domain.laptop.GlareType
import going9.laptopgg.domain.laptop.PanelType

class LaptopRequest(
    // 이미지 링크
    val imgLink: String,

    // 가격
    val price: Int,
    val priceLink: String,

    // 랩탑 속성
    val name: String,
    val manufacturer: String,
    val mainCategory: String,   // 대분류
    val subCategory: String,    // 하위 분류
    val weight: Int,
    val thunderVolt: Int? = null,
    val usb4: Int? = null,
    val battery: Int,
    val sdCard: String,  // 마이크로, 풀사이즈

    // cpu
    val cpu: List<Long>,

    // gpu
    val gpu: List<Long>,
    val tgp: Int? = null,
    val isMux: Boolean,

    // RAM 고정 속성
    val ramSlot: Int? = null,  // RAM 슬롯 수, 업그레이드 가능 시 슬롯 수 입력 불가능하면 null
    val clockSpeed: Int,  // 램 클럭 스피드
    val ddrType: Int, // ddr4, ddr5, lpddr

    // RAM 가변 속성
    val ramCapacity: List<Int>,

    // 디스플레이 속성
    val panel: PanelType,
    val resolutionWidth: Int,  // 가로 해상도
    val resolutionHeight: Int, // 세로 해상도
    val brightness: Int,  // 밝기 (니트)
    val colorAccuracy: ColorAccuracy,  // 색 정확도
    val refreshRate: Int,  // 주사율 (Hz)
    val glareType: GlareType,  // 글레어 유형 (NONE, GLARE, ANTI_GLARE)
    val screenSize: Double,  // 화면 크기 (인치 단위, 예: 15.6, 16 등)
    val isTouch: Boolean,  // 터치 스크린 여부
    val aspectRatio: String,  // 화면 비율 (예: "16:9", "16:10")

    // 스토리지 고유 속성
    val storageSlot: Int? = null,

    // 스토리지 가변 속성
    val storageCapacity: List<Int>
) {
    companion object {
        // 기본값을 가진 LaptopRequest 인스턴스를 생성하는 정적 팩토리 메서드
        fun default(): LaptopRequest {
            return LaptopRequest(
                imgLink = "",
                price = 0,
                priceLink = "",
                name = "",
                manufacturer = "",
                mainCategory = "",
                subCategory = "",
                weight = 0,
                thunderVolt = null,
                usb4 = null,
                battery = 0,
                sdCard = "",
                cpu = listOf(),
                gpu = listOf(),
                tgp = null,
                isMux = false,
                ramSlot = null,
                clockSpeed = 0,
                ddrType = 0,
                ramCapacity = listOf(),
                panel = PanelType.IPS,
                resolutionWidth = 0,
                resolutionHeight = 0,
                brightness = 0,
                colorAccuracy = ColorAccuracy.GOOD,
                refreshRate = 0,
                glareType = GlareType.NONE,
                screenSize = 0.0,
                isTouch = false,
                aspectRatio = "",
                storageSlot = null,
                storageCapacity = listOf()
            )
        }
    }
}


