package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.PortabilityTier

data class ProfileMetrics(
    val batteryTier: BatteryTier,
    val portabilityTier: PortabilityTier,
    val batteryCapacityScore: Int,
    val portabilityScore: Int,
    val displayScore: Int,
    val ramScore: Int,
    val tgpScore: Int,
    val usageBoosts: UsageBoosts,
)

data class UsageBoosts(
    val officeBoost: Int,
    val portableBoost: Int,
    val creatorBoost: Int,
    val gameBoost: Int,
)

class ProfileMetricPolicy(
    private val displayMetricPolicy: DisplayMetricPolicy = DisplayMetricPolicy(),
) {
    fun calculate(laptop: PersistedCrawledLaptopSnapshot, gpu: GpuInsights): ProfileMetrics {
        return ProfileMetrics(
            batteryTier = batteryTier(laptop.batteryCapacity),
            portabilityTier = portabilityTier(laptop.weight),
            batteryCapacityScore = batteryCapacityScore(laptop.batteryCapacity),
            portabilityScore = portabilityScore(laptop.weight),
            displayScore = displayMetricPolicy.displayScore(laptop),
            ramScore = ramScore(laptop.ramSize),
            tgpScore = tgpScore(laptop.tgp, gpu.isIntegrated),
            usageBoosts = usageBoosts(laptop),
        )
    }

    fun portabilityScore(weight: Double?): Int {
        val value = weight ?: return 40
        return when {
            value <= 0.9 -> 100
            value <= 1.1 -> 95
            value <= 1.3 -> 90
            value <= 1.5 -> 82
            value <= 1.7 -> 74
            value <= 2.0 -> 60
            value <= 2.3 -> 45
            value <= 2.6 -> 30
            else -> 15
        }
    }

    fun portabilityTier(weight: Double?): PortabilityTier {
        val value = weight ?: return PortabilityTier.UNKNOWN
        return when {
            value <= 1.0 -> PortabilityTier.TABLET_LIGHT
            value <= 1.3 -> PortabilityTier.ULTRALIGHT
            value <= 1.6 -> PortabilityTier.LIGHT
            value <= 2.2 -> PortabilityTier.BALANCED
            else -> PortabilityTier.HEAVY
        }
    }

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

    fun ramScore(ramSize: Int?): Int {
        val value = ramSize ?: return 30
        return when {
            value >= 64 -> 100
            value >= 48 -> 96
            value >= 32 -> 92
            value >= 24 -> 82
            value >= 16 -> 70
            value >= 12 -> 50
            value >= 8 -> 35
            else -> 20
        }
    }

    fun tgpScore(tgp: Int?, isIntegrated: Boolean): Int {
        if (isIntegrated) {
            return 25
        }

        val value = tgp?.takeIf { it > 0 } ?: return 40
        return when {
            value >= 150 -> 100
            value >= 130 -> 90
            value >= 110 -> 82
            value >= 90 -> 72
            value >= 70 -> 60
            value >= 50 -> 48
            else -> 35
        }
    }

    private fun usageBoosts(laptop: PersistedCrawledLaptopSnapshot): UsageBoosts {
        val usages = laptop.usages.map { it.trim() }.toSet()
        return UsageBoosts(
            officeBoost = if ("사무/인강용" in usages) 8 else 0,
            portableBoost = if ("휴대용" in usages) 8 else 0,
            creatorBoost = if ("그래픽작업용" in usages) 8 else 0,
            gameBoost = if ("게임용" in usages) 8 else 0,
        )
    }

}
