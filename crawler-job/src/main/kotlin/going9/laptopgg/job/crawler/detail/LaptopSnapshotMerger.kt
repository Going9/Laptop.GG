package going9.laptopgg.job.crawler.detail

import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.job.crawler.list.ProductCard
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class LaptopSnapshotMerger(
    private val crawledCpuModelResolver: CrawledCpuModelResolver,
    private val crawledGraphicsModelResolver: CrawledGraphicsModelResolver,
) {
    internal fun createCommand(
        productCard: ProductCard,
        parsedSpecTable: ParsedSpecTable,
        summaryFallback: SummaryFallback,
    ): CrawledLaptopCommand {
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

        return CrawledLaptopCommand(
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
                ?: if (crawledGraphicsModelResolver.isIntegrated(gpuKind, gpuModel)) 0 else null,
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
            usages = usages.distinct(),
        )
    }

    internal fun resolveCpuModel(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return crawledCpuModelResolver.resolve(rawCpu, cpuManufacturer, productName)
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

    private companion object {
        const val DEFAULT_REFRESH_RATE = 60
    }
}
