package going9.laptopgg.application.laptop

import going9.laptopgg.domain.laptop.Laptop

data class LaptopDetailResult(
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
        fun from(laptop: Laptop): LaptopDetailResult {
            return LaptopDetailResult(
                id = laptop.id!!,
                name = laptop.name,
                imageUrl = laptop.imageUrl,
                manufacturer = laptop.name.substringBefore(" "),
                detailPage = laptop.detailPage,
                price = laptop.price,
                cpuManufacturer = laptop.cpuManufacturer,
                cpu = laptop.cpu,
                os = humanizeOs(laptop.os),
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
                usage = laptop.laptopUsage.map { it.usage },
            )
        }

        private fun humanizeOs(rawOs: String?): String? {
            val trimmed = rawOs?.trim()?.takeIf { it.isNotBlank() } ?: return null
            val normalized = trimmed.lowercase()

            return when {
                normalized == "freedos" || normalized == "free dos" || normalized == "dos" -> "프리도스"
                normalized.contains("windows") -> trimmed
                    .replace(Regex("(?i)windows"), "윈도우")
                    .replace(Regex("(?i)home"), "홈")
                    .replace(Regex("(?i)pro"), "프로")
                normalized.contains("macos") || normalized.contains("mac os") ->
                    trimmed.replace(Regex("(?i)mac\\s?os"), "macOS")
                normalized.contains("linux") -> trimmed.replace(Regex("(?i)linux"), "리눅스")
                else -> trimmed
            }
        }
    }
}
