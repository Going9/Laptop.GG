package going9.laptopgg.dto.response

import going9.laptopgg.domain.laptop.Laptop

data class LaptopDetailResponse(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val manufacturer: String,
    val detailPage: String,
    var price: Int?,
    val cpuManufacturer: String?,
    val cpu: String?,
    val os: String?,
    val screenSize: Int?,  // 화면 크기 (인치)
    val resolution: String?, // 해상도
    val brightness: Int?,    // 밝기 (니트)
    val refreshRate: Int?,   // 주사율 (Hz)
    val ramSize: Int?,        // 램 용량 (GB)
    val ramType: String?,     // 램 타입 (예: DDR4)
    val isRamReplaceable: Boolean?,    // 램 교체 가능 여부
    val graphicsType: String?,       // 그래픽 타입 (내장/외장)
    val tgp: Int?,                  // TGP (W) (외장 그래픽에만 적용)
    val thunderboltCount: Int?,     // 썬더볼트 포트 개수
    val usbCCount: Int?,            // USB-C 포트 개수
    val usbACount: Int?,            // USB-A 포트 개수
    val sdCard: String?,
    val isSupportsPdCharging: Boolean?, // USB PD 충전 지원 여부
    val batteryCapacity: Double?,      // 배터리 용량 (Wh)
    val storageCapacity: Int?,       // 저장 장치 용량 (GB)
    val storageSlotCount: Int?,     // 저장 장치 슬롯 수
    val weight: Double?,             // 무게 (kg)
    val usage: List<String>
) {
    companion object {
        fun of(laptop: Laptop): LaptopDetailResponse {
            return LaptopDetailResponse(
                id = laptop.id!!,
                name = laptop.name,
                imageUrl = laptop.imageUrl,
                manufacturer = laptop.name.substringBefore(" "),
                detailPage = laptop.detailPage,
                price = laptop.price,
                cpuManufacturer = laptop.cpuManufacturer,
                cpu = laptop.cpu,
                os = laptop.os,
                screenSize = laptop.screenSize,
                resolution = laptop.resolution,
                brightness = laptop.brightness,
                refreshRate = laptop.refreshRate,
                ramSize = laptop.ramSize,
                ramType = laptop.ramType,
                isRamReplaceable = laptop.isRamReplaceable,
                graphicsType = laptop.graphicsType,
                tgp = laptop.tgp,
                thunderboltCount = laptop.thunderboltCount,
                usbCCount = laptop.usbCCount,
                usbACount = laptop.usbACount,
                sdCard = laptop.sdCard,
                isSupportsPdCharging = laptop.isSupportsPdCharging,
                batteryCapacity = laptop.batteryCapacity,
                storageCapacity = laptop.storageCapacity,
                storageSlotCount = laptop.storageSlotCount,
                weight = laptop.weight,
                usage = laptop.laptopUsage.map { it.usage }
            )
        }
    }
}