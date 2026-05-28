package going9.laptopgg.job.crawler.detail

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.job.crawler.list.ProductCard

internal data class BuildLaptopResult(
    val command: CrawledLaptopCommand,
    val degradationReasons: List<String>,
) {
    val isDegraded: Boolean
        get() = degradationReasons.isNotEmpty()
}

internal data class DetailRequestContext(
    val makerName: String?,
    val productName: String?,
    val prodType: String?,
)

internal data class ParsedSpecTable(
    val values: Map<String, String>,
    val usages: List<String>,
)

internal data class SummaryFallback(
    val cpuManufacturer: String? = null,
    val cpu: String? = null,
    val os: String? = null,
    val screenSize: Int? = null,
    val resolution: String? = null,
    val brightness: Int? = null,
    val refreshRate: Int? = null,
    val ramSize: Int? = null,
    val ramType: String? = null,
    val isRamReplaceable: Boolean? = null,
    val graphicsKind: String? = null,
    val graphicsModel: String? = null,
    val tgp: Int? = null,
    val isSupportsPdCharging: Boolean? = null,
    val batteryCapacity: Double? = null,
    val storageCapacity: Int? = null,
    val storageSlotCount: Int? = null,
    val weight: Double? = null,
    val usages: List<String> = emptyList(),
)

internal data class DetailRefreshWorkItem(
    val productCard: ProductCard,
    val existingLaptop: ExistingCrawledLaptopSnapshot?,
)

internal data class DetailRefreshOutcome(
    val workItem: DetailRefreshWorkItem,
    val buildResult: BuildLaptopResult? = null,
    val error: Exception? = null,
)
