package going9.laptopgg.application.crawler

import java.time.LocalDateTime

data class CrawledProductCardCommand(
    val productCode: String,
    val productName: String,
    val detailPage: String,
    val imageUrl: String,
    val price: Int?,
)

data class CrawledLaptopCommand(
    val name: String,
    val imageUrl: String,
    val detailPage: String,
    val productCode: String?,
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
    val lastDetailedCrawledAt: LocalDateTime?,
    val usages: List<String>,
)

data class UpdateCrawledLaptopCommand(
    val name: String? = null,
    val imageUrl: String? = null,
    val detailPage: String? = null,
    val productCode: String? = null,
    val price: Int? = null,
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
    val graphicsType: String? = null,
    val tgp: Int? = null,
    val thunderboltCount: Int? = null,
    val usbCCount: Int? = null,
    val usbACount: Int? = null,
    val sdCard: String? = null,
    val isSupportsPdCharging: Boolean? = null,
    val batteryCapacity: Double? = null,
    val storageCapacity: Int? = null,
    val storageSlotCount: Int? = null,
    val weight: Double? = null,
    val lastDetailedCrawledAt: LocalDateTime? = null,
    val usages: List<String>? = null,
)

data class PersistedCrawledLaptopSnapshot(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val detailPage: String,
    val productCode: String?,
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
    val lastDetailedCrawledAt: LocalDateTime?,
    val usages: List<String>,
)

data class ExistingCrawledLaptopSnapshot(
    val id: Long,
    val productCode: String?,
    val detailPage: String,
    val cpuManufacturer: String?,
    val cpu: String?,
    val os: String?,
    val screenSize: Int?,
    val resolution: String?,
    val ramSize: Int?,
    val graphicsType: String?,
    val storageCapacity: Int?,
    val batteryCapacity: Double?,
    val weight: Double?,
    val lastDetailedCrawledAt: LocalDateTime?,
    val usageCount: Int,
)

data class ExistingCrawledLaptopLookup(
    val byProductCode: Map<String, ExistingCrawledLaptopSnapshot>,
    val byDetailPage: Map<String, ExistingCrawledLaptopSnapshot>,
) {
    fun find(productCard: CrawledProductCardCommand): ExistingCrawledLaptopSnapshot? {
        return byProductCode[productCard.productCode]
            ?: byDetailPage[productCard.detailPage]
    }
}

data class RecordPriceHistoryCommand(
    val laptopId: Long,
    val price: Int,
    val capturedAt: LocalDateTime,
)

data class UpsertRecommendationScoreCommand(
    val laptopId: Long,
    val useCase: String,
    val gateScore: Int,
    val staticScore: Double,
    val budgetWeight: Double,
    val updatedAt: LocalDateTime,
)

data class CreateCrawlerRunCommand(
    val filterProfile: String,
    val startPage: Int,
    val limitCount: Int?,
    val status: CrawlerRunStatusResult = CrawlerRunStatusResult.RUNNING,
    val endedAt: LocalDateTime? = null,
    val errorMessage: String? = null,
)

data class UpdateCrawlerRunCommand(
    val runId: Long,
    val status: CrawlerRunStatusResult,
    val processedCount: Int? = null,
    val createdCount: Int? = null,
    val updatedCount: Int? = null,
    val degradedCount: Int? = null,
    val failedCount: Int? = null,
    val failureSamples: String? = null,
    val errorMessage: String? = null,
    val endedAt: LocalDateTime,
)

data class CrawlerRunState(
    val id: Long?,
    val status: CrawlerRunStatusResult,
)

enum class SaveResult {
    CREATED,
    UPDATED,
    UNCHANGED,
}
