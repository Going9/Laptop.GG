package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.Laptop
import java.time.LocalDateTime

internal object DetailRefreshPolicy {
    private const val DETAIL_REFRESH_INTERVAL_DAYS = 30L

    fun needsRefresh(existingLaptop: Laptop, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val lastDetailedCrawledAt = existingLaptop.lastDetailedCrawledAt
        return existingLaptop.cpuManufacturer.isNullOrBlank() ||
            existingLaptop.cpu.isNullOrBlank() ||
            existingLaptop.os.isNullOrBlank() ||
            existingLaptop.screenSize == null ||
            existingLaptop.resolution.isNullOrBlank() ||
            existingLaptop.ramSize == null ||
            existingLaptop.graphicsType.isNullOrBlank() ||
            existingLaptop.storageCapacity == null ||
            existingLaptop.batteryCapacity == null ||
            existingLaptop.weight == null ||
            lastDetailedCrawledAt == null ||
            lastDetailedCrawledAt.isBefore(now.minusDays(DETAIL_REFRESH_INTERVAL_DAYS)) ||
            existingLaptop.laptopUsage.isEmpty()
    }
}
