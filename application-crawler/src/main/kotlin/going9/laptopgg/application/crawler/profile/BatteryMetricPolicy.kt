package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.BatteryTier

class BatteryMetricPolicy {
    fun batteryCapacityScore(batteryCapacity: Double?): Int {
        val value = batteryCapacity ?: return 35
        return when {
            value >= 90 -> 100
            value >= 80 -> 92
            value >= 70 -> 82
            value >= 60 -> 70
            value >= 50 -> 58
            value >= 40 -> 45
            else -> 30
        }
    }

    fun batteryTier(batteryCapacity: Double?): BatteryTier {
        val value = batteryCapacity ?: return BatteryTier.UNKNOWN
        return when {
            value >= 85 -> BatteryTier.VERY_HIGH
            value >= 70 -> BatteryTier.HIGH
            value >= 55 -> BatteryTier.MEDIUM
            value >= 40 -> BatteryTier.LOW
            else -> BatteryTier.VERY_LOW
        }
    }
}
