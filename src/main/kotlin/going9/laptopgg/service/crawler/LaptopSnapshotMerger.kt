package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.service.LaptopProfileFactory
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class LaptopSnapshotMerger(
    private val laptopProfileFactory: LaptopProfileFactory,
) {
    internal fun createLaptop(
        productCard: ProductCard,
        parsedSpecTable: ParsedSpecTable,
        summaryFallback: SummaryFallback,
    ): Laptop {
        val spec = parsedSpecTable.values
        val rawCpu = spec["CPU 넘버"]?.substringBefore(" (") ?: summaryFallback.cpu
        val cpuManufacturer = resolveCpuManufacturer(
            rawManufacturer = spec["CPU 제조사"] ?: summaryFallback.cpuManufacturer,
            productName = productCard.productName,
            rawCpu = rawCpu,
        )
        val cpu = resolveCpuModel(
            rawCpu = rawCpu,
            cpuManufacturer = cpuManufacturer,
            productName = productCard.productName,
        )
        val gpuKind = spec["GPU 종류"] ?: summaryFallback.graphicsKind
        val gpuModel = spec["GPU 칩셋"] ?: summaryFallback.graphicsModel ?: gpuKind
        val usages = parsedSpecTable.usages.ifEmpty { summaryFallback.usages }

        val laptop = Laptop(
            name = productCard.productName,
            imageUrl = productCard.imageUrl,
            detailPage = productCard.detailPage,
            productCode = productCard.productCode,
            price = productCard.price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = DanawaDetailParser.normalizeOs(spec["운영체제(OS)"] ?: summaryFallback.os),
            screenSize = DanawaDetailParser.parseScreenSize(spec["화면 크기"]) ?: summaryFallback.screenSize,
            resolution = spec["해상도"] ?: summaryFallback.resolution,
            brightness = DanawaDetailParser.parseIntValue(spec["밝기"]) ?: summaryFallback.brightness,
            refreshRate = DanawaDetailParser.parseIntValue(spec["주사율"]) ?: summaryFallback.refreshRate ?: DEFAULT_REFRESH_RATE,
            ramSize = DanawaDetailParser.parseCapacityInGb(spec["램"]) ?: summaryFallback.ramSize,
            ramType = spec["램 타입"] ?: summaryFallback.ramType,
            isRamReplaceable = DanawaDetailParser.parsePossible(spec["램 교체"]) ?: summaryFallback.isRamReplaceable,
            graphicsType = gpuModel,
            tgp = DanawaDetailParser.parseIntValue(spec["TGP"])
                ?: summaryFallback.tgp
                ?: if (isIntegratedGraphics(gpuKind, gpuModel)) 0 else null,
            thunderboltCount = DanawaDetailParser.parseThunderboltCount(spec),
            usbCCount = DanawaDetailParser.parseUsbCCount(spec),
            usbACount = DanawaDetailParser.parseCountValue(spec["USB-A"]),
            sdCard = DanawaDetailParser.parseSdCard(spec),
            isSupportsPdCharging = when {
                spec["전원"] != null -> spec["전원"]!!.contains("USB-PD", ignoreCase = true)
                else -> summaryFallback.isSupportsPdCharging
            },
            batteryCapacity = DanawaDetailParser.parseDoubleValue(spec["배터리"]) ?: summaryFallback.batteryCapacity,
            storageCapacity = DanawaDetailParser.parseCapacityInGb(spec["용량"]) ?: summaryFallback.storageCapacity,
            storageSlotCount = DanawaDetailParser.parseCountValue(spec["저장 슬롯"]) ?: summaryFallback.storageSlotCount,
            weight = DanawaDetailParser.parseWeightValue(spec["무게"]) ?: summaryFallback.weight,
            lastDetailedCrawledAt = LocalDateTime.now(),
            laptopUsage = mutableListOf(),
        )

        laptop.laptopUsage = usages
            .distinct()
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()

        return laptop
    }

    internal fun resolveCpuModel(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return laptopProfileFactory.resolveCpuToken(rawCpu, cpuManufacturer, productName)
            ?: rawCpu?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun resolveCpuManufacturer(rawManufacturer: String?, productName: String, rawCpu: String?): String? {
        rawManufacturer?.let(DanawaDetailParser::normalizeCpuManufacturer)?.let { return it }

        val normalizedName = productName.uppercase()
        val normalizedCpu = rawCpu.orEmpty().uppercase()

        return when {
            normalizedName.contains("APPLE") || normalizedName.contains("맥북") -> "애플(ARM)"
            normalizedName.contains("SNAPDRAGON") ||
                normalizedName.contains("X ELITE") ||
                normalizedName.contains("X PLUS") ||
                normalizedCpu.startsWith("X1") ||
                normalizedCpu.startsWith("X2") -> "퀄컴"
            else -> null
        }
    }

    private fun isIntegratedGraphics(graphicsKind: String?, graphicsModel: String?): Boolean {
        val normalizedKind = graphicsKind.orEmpty().uppercase()
        val normalizedModel = graphicsModel.orEmpty().uppercase()

        if (normalizedKind.contains("외장")) {
            return false
        }
        if (normalizedKind.contains("내장")) {
            return true
        }

        if (DISCRETE_GPU_KEYWORDS.any { normalizedModel.contains(it) }) {
            return false
        }
        if (INTEGRATED_GPU_KEYWORDS.any { normalizedModel.contains(it) }) {
            return true
        }

        return false
    }

    private companion object {
        const val DEFAULT_REFRESH_RATE = 60
        val DISCRETE_GPU_KEYWORDS = listOf(
            "RTX",
            "GTX",
            "GEFORCE",
            "RTX PRO",
            "RTX A",
            "ARC B",
            "ARC A",
            "RADEON RX",
        )
        val INTEGRATED_GPU_KEYWORDS = listOf(
            "INTEL GRAPHICS",
            "IRIS",
            "UHD",
            "HD GRAPHICS",
            "ARC 130T",
            "ARC 140T",
            "RADEON 890M",
            "RADEON 880M",
            "RADEON 860M",
            "RADEON 840M",
            "RADEON 820M",
            "RADEON 8060S",
            "RADEON 780M",
            "RADEON 760M",
            "RADEON 740M",
            "RADEON 680M",
            "RADEON 660M",
            "RADEON 610M",
            "RADEON GRAPHICS",
            "ADRENO",
        )
    }
}
