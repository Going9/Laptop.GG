package going9.laptopgg.web.dto.response

import going9.laptopgg.application.laptop.LaptopDetailResult

data class LaptopDetailResponse(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val manufacturer: String,
    val detailPage: String,
    val price: Int?,
    val cpuManufacturer: String?,
    val cpu: String?,
    val os: String?,
    val screenSize: Int?,
    val resolution: String?,
    val brightness: Int?,
    val refreshRate: Int?,
    val ramSize: Int?,
    val ramType: String?,
    val isRamReplaceable: Boolean?,
    val graphicsType: String?,
    val tgp: Int?,
    val thunderboltCount: Int?,
    val usbCCount: Int?,
    val usbACount: Int?,
    val sdCard: String?,
    val isSupportsPdCharging: Boolean?,
    val batteryCapacity: Double?,
    val storageCapacity: Int?,
    val storageSlotCount: Int?,
    val weight: Double?,
    val usage: List<String>,
) {
    companion object {
        fun from(result: LaptopDetailResult): LaptopDetailResponse {
            return LaptopDetailResponse(
                id = result.id,
                name = result.name,
                imageUrl = result.imageUrl,
                manufacturer = result.manufacturer,
                detailPage = result.detailPage,
                price = result.price,
                cpuManufacturer = result.cpuManufacturer,
                cpu = result.cpu,
                os = result.os,
                screenSize = result.screenSize,
                resolution = result.resolution,
                brightness = result.brightness,
                refreshRate = result.refreshRate,
                ramSize = result.ramSize,
                ramType = result.ramType,
                isRamReplaceable = result.isRamReplaceable,
                graphicsType = result.graphicsType,
                tgp = result.tgp,
                thunderboltCount = result.thunderboltCount,
                usbCCount = result.usbCCount,
                usbACount = result.usbACount,
                sdCard = result.sdCard,
                isSupportsPdCharging = result.isSupportsPdCharging,
                batteryCapacity = result.batteryCapacity,
                storageCapacity = result.storageCapacity,
                storageSlotCount = result.storageSlotCount,
                weight = result.weight,
                usage = result.usage,
            )
        }
    }
}
