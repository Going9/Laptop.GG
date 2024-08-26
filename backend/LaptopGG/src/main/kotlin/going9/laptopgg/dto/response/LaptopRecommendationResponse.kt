package going9.laptopgg.dto.response

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.domain.laptop.Gpu

class LaptopRecommendationResponse(
    val manufacturer: String,
    val price: Int,
    val name: String,
    val weight: Int,
    val thunderBoltPorts: Int,
    val usb4Ports: Int,
    val sdCardType: String,
    val isTenKey: Boolean,
    val cpu: List<CpuResponse>,
    val gpu: List<GpuResponse>,
    val ramSlot: Int,
    val ramCapacity: List<Int>,
    val ramClockSpeed: Int,
    val ramDdrType: String, // string 으로 수정 필요
    val displays: List<DisplayResponse>,
    val storageSlot: Int,
    val storageCapacity: List<Int>
) {
}