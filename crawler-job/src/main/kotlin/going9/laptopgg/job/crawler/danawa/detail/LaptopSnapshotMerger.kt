package going9.laptopgg.job.crawler.danawa.detail

import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.job.crawler.list.ProductCard
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
internal class LaptopSnapshotMerger(
    private val crawledCpuManufacturerResolver: CrawledCpuManufacturerResolver,
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
        val cpuManufacturer = crawledCpuManufacturerResolver.resolve(
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
            os = DanawaSpecValueParser.normalizeOs(spec["운영체제(OS)"] ?: summaryFallback.os),
            screenSize = DanawaSpecValueParser.parseScreenSize(spec["화면 크기"]) ?: summaryFallback.screenSize,
            resolution = spec["해상도"] ?: summaryFallback.resolution,
            brightness = DanawaSpecValueParser.parseIntValue(spec["밝기"]) ?: summaryFallback.brightness,
            refreshRate = DanawaSpecValueParser.parseIntValue(spec["주사율"]) ?: summaryFallback.refreshRate ?: DEFAULT_REFRESH_RATE,
            ramSize = DanawaSpecValueParser.parseCapacityInGb(spec["램"]) ?: summaryFallback.ramSize,
            ramType = spec["램 타입"] ?: summaryFallback.ramType,
            isRamReplaceable = DanawaSpecValueParser.parsePossible(spec["램 교체"]) ?: summaryFallback.isRamReplaceable,
            graphicsType = gpuModel,
            tgp = DanawaSpecValueParser.parseIntValue(spec["TGP"])
                ?: summaryFallback.tgp
                ?: if (crawledGraphicsModelResolver.isIntegrated(gpuKind, gpuModel)) 0 else null,
            thunderboltCount = DanawaSpecValueParser.parseThunderboltCount(spec),
            usbCCount = DanawaSpecValueParser.parseUsbCCount(spec),
            usbACount = DanawaSpecValueParser.parseCountValue(spec["USB-A"]),
            sdCard = DanawaSpecValueParser.parseSdCard(spec),
            isSupportsPdCharging = when {
                spec["전원"] != null -> spec["전원"]!!.contains("USB-PD", ignoreCase = true)
                else -> summaryFallback.isSupportsPdCharging
            },
            batteryCapacity = DanawaSpecValueParser.parseDoubleValue(spec["배터리"]) ?: summaryFallback.batteryCapacity,
            storageCapacity = DanawaSpecValueParser.parseCapacityInGb(spec["용량"]) ?: summaryFallback.storageCapacity,
            storageSlotCount = DanawaSpecValueParser.parseCountValue(spec["저장 슬롯"]) ?: summaryFallback.storageSlotCount,
            weight = DanawaSpecValueParser.parseWeightValue(spec["무게"]) ?: summaryFallback.weight,
            lastDetailedCrawledAt = LocalDateTime.now(),
            usages = usages.distinct(),
        )
    }

    internal fun resolveCpuModel(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return crawledCpuModelResolver.resolve(rawCpu, cpuManufacturer, productName)
    }

    private companion object {
        const val DEFAULT_REFRESH_RATE = 60
    }
}
