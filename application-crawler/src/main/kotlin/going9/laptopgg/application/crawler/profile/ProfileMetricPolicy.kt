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
    private val batteryMetricPolicy: BatteryMetricPolicy = BatteryMetricPolicy(),
    private val mobilityMetricPolicy: MobilityMetricPolicy = MobilityMetricPolicy(),
    private val displayMetricPolicy: DisplayMetricPolicy = DisplayMetricPolicy(),
) {
    fun calculate(laptop: PersistedCrawledLaptopSnapshot, gpu: GpuInsights): ProfileMetrics {
        return ProfileMetrics(
            batteryTier = batteryMetricPolicy.batteryTier(laptop.batteryCapacity),
            portabilityTier = mobilityMetricPolicy.portabilityTier(laptop.weight),
            batteryCapacityScore = batteryMetricPolicy.batteryCapacityScore(laptop.batteryCapacity),
            portabilityScore = mobilityMetricPolicy.portabilityScore(laptop.weight),
            displayScore = displayMetricPolicy.displayScore(laptop),
            ramScore = ramScore(laptop.ramSize),
            tgpScore = tgpScore(laptop.tgp, gpu.isIntegrated),
            usageBoosts = usageBoosts(laptop),
        )
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
