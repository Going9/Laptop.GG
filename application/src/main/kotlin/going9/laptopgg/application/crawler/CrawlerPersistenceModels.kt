package going9.laptopgg.application.crawler

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
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
) {
    fun toLaptop(): Laptop {
        val laptop = Laptop(
            name = name,
            imageUrl = imageUrl,
            detailPage = detailPage,
            productCode = productCode,
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = os,
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
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            laptopUsage = mutableListOf(),
        )
        laptop.laptopUsage = usages
            .distinct()
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()
        return laptop
    }
}

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

enum class SaveResult {
    CREATED,
    UPDATED,
    UNCHANGED,
}
