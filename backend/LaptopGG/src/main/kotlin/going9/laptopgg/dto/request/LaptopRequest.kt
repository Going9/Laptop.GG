package going9.laptopgg.dto.request

class LaptopRequest(
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

    // cpu
    val cpus: List<Long> = listOf(),

    // gpu
    val gpus: List<LaptopGpuRequest> = listOf(),

    // ram
    val rams: List<RamRequest> = listOf(),

    // display
    val displays: List<DisplayRequest> = listOf(),

    // storage
    val storages: List<StorageRequest> = listOf(),
)
