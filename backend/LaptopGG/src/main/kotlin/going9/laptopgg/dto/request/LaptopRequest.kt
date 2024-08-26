package going9.laptopgg.dto.request

data class LaptopRequest(
    // 이미지 링크
    val imgLink: String = "",

    // 가격
    val price: Int = 0,
    val priceLink: String = "",

    // 랩탑 속성
    val name: String = "",
    val manufacturer: String = "",
    val mainCategory: String = "",   // 대분류
    val subCategory: String = "",    // 하위 분류
    val weight: Int = 0,
    val thunderBoltPorts: Int? = null,
    val usb4Ports: Int? = null,
    val batteryCapacity: Int = 0,
    val sdCardType: String = "",  // 마이크로, 풀사이즈
    val isTenKey: Boolean = false,

    // cpu
    val cpus: List<Long> = mutableListOf(),

    // gpu
    val gpus: List<LaptopGpuRequest> = mutableListOf(),

    // ram
    val rams: List<RamRequest> = mutableListOf(),

    // display
    val displays: List<DisplayRequest> = mutableListOf(),

    // storage
    val storages: List<StorageRequest> = mutableListOf(),
)

