package going9.laptopgg.application.laptop

import going9.laptopgg.application.laptop.port.LaptopDetailRecord
import going9.laptopgg.application.laptop.port.LaptopPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort

class GetLaptopDetailUseCase(
    private val laptopPort: LaptopPort,
    private val transactionPort: ApplicationTransactionPort,
) {
    fun get(laptopId: Long): LaptopDetailResult {
        return transactionPort.read {
            val laptop = laptopPort.findDetailById(laptopId) ?: throw IllegalArgumentException("Laptop not found: $laptopId")
            laptop.toResult()
        }
    }

    private fun LaptopDetailRecord.toResult(): LaptopDetailResult {
        return LaptopDetailResult(
            id = id,
            name = name,
            imageUrl = imageUrl,
            manufacturer = name.substringBefore(" "),
            detailPage = detailPage,
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = humanizeOs(os),
            screenSize = screenSize,
            resolution = resolution,
            brightness = brightness,
            refreshRate = refreshRate,
            ramSize = ramSize,
            ramType = ramType,
            isRamReplaceable = isRamReplaceable,
            graphicsType = graphicsType,
            tgp = tgp,
            thunderboltCount = thunderboltCount,
            usbCCount = usbCCount,
            usbACount = usbACount,
            sdCard = sdCard,
            isSupportsPdCharging = isSupportsPdCharging,
            batteryCapacity = batteryCapacity,
            storageCapacity = storageCapacity,
            storageSlotCount = storageSlotCount,
            weight = weight,
            usage = usage,
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
