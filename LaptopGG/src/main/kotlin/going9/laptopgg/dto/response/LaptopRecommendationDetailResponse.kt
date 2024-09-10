package going9.laptopgg.dto.response

data class LaptopRecommendationDetailResponse(
    val id: Long,
    val imgLink: String,
    val priceLink: String,
    val batteryCapacity: Int,
    val manufacturer: String,
    val price: Int,
    val name: String,
    val weight: Double,
    val thunderBoltPorts: Int?,
    val usb4Ports: Int?,
    val sdCardType: String,
    val isTenKey: Boolean,
    val cpus: List<String>,
    val gpus: List<String>,
    val ramSlot: Int?,
    val ramCapacity: List<Int>,
    val ramClockSpeed: Int,
    val ramDdrType: String, // string 으로 수정 필요
    val displays: List<DisplayResponse>,
    val storageSlot: Int?,
    val storageCapacity: List<Int>
) {
}
