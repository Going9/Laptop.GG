package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.ColorAccuracy
import going9.laptopgg.domain.laptop.GlareType
import going9.laptopgg.domain.laptop.PanelType

class LaptopRequest(
    // 랩탑 속성
    val name: String,
    val manufacturer: String,
    val weight: Int,
    val thunderVolt: Int? = null,
    val usb4: Int? = null,
    val battery: Int,
    val sdCard: String,  // 마이크로, 풀사이즈

    // cpu
    val cpu: List<Long>,

    // gpu
    val gpu: List<Long>,

    // RAM 고정 속성
    val ramSlot: Int? = null,  // RAM 슬롯 수, 업그레이드 가능 시 슬롯 수 입력 불가능하면 null
    val clockSpeed: Int,  // 램 클럭 스피드
    val ddrType: Int, // drr4 or 5 or lpddr

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
    val storageCapacity: List<Int>,
)
