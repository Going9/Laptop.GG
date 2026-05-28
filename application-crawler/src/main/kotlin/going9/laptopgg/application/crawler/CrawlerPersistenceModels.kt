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

enum class SaveResult {
    CREATED,
    UPDATED,
    UNCHANGED,
}
