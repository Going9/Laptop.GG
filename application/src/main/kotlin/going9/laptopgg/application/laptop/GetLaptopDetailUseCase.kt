package going9.laptopgg.application.laptop

import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
class GetLaptopDetailUseCase(
    private val laptopPort: LaptopPort,
) {
    fun get(laptopId: Long): LaptopDetailResult {
        val laptop = laptopPort.findWithUsageById(laptopId) ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        return laptop.toDetailResult()
    }

    private fun Laptop.toDetailResult(): LaptopDetailResult {
        return LaptopDetailResult(
            id = requireNotNull(id) { "Persisted laptop id must not be null." },
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
            usage = laptopUsage.map { it.usage },
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
