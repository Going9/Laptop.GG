package going9.laptopgg.application.crawler

import going9.laptopgg.domain.laptop.BatteryTier
import going9.laptopgg.domain.laptop.PortabilityTier
import kotlin.math.roundToInt

data class ProfileScores(
    val batteryTier: BatteryTier,
    val portabilityTier: PortabilityTier,
    val officeScore: Int,
    val batteryScore: Int,
    val casualGameScore: Int,
    val onlineGameScore: Int,
    val aaaGameScore: Int,
    val creatorScore: Int,
    val portabilityScore: Int,
    val displayScore: Int,
    val ramScore: Int,
    val tgpScore: Int,
)

class ProfileScorePolicy {
    fun calculate(laptop: PersistedCrawledLaptopSnapshot, cpu: CpuInsights, gpu: GpuInsights): ProfileScores {
        val portabilityScore = portabilityScore(laptop.weight)
        val batteryCapacityScore = batteryCapacityScore(laptop.batteryCapacity)
        val ramScore = ramScore(laptop.ramSize)
        val displayScore = displayScore(laptop)
        val tgpScore = tgpScore(laptop.tgp, gpu.isIntegrated)
        val usageBoost = usageSet(laptop)

        val officeScore = clampScore(
            cpu.performanceScore * 0.40 +
                ramScore * 0.20 +
                displayScore * 0.15 +
                portabilityScore * 0.15 +
                cpu.lowPowerScore * 0.10 +
                usageBoost.officeBoost,
        )

        val batteryScore = clampScore(
            batteryCapacityScore * 0.60 +
                cpu.lowPowerScore * 0.25 +
                portabilityScore * 0.15 +
                usageBoost.portableBoost,
        )

        val casualGameScore = clampScore(
            gpu.performanceScore * 0.45 +
                cpu.performanceScore * 0.20 +
                ramScore * 0.15 +
                displayScore * 0.10 +
                tgpScore * 0.10 +
                usageBoost.gameBoost,
        )

        val onlineGameScore = clampScore(
            gpu.performanceScore * 0.55 +
                cpu.performanceScore * 0.20 +
                ramScore * 0.10 +
                tgpScore * 0.15 +
                usageBoost.gameBoost,
        )

        val aaaGameScore = clampScore(
            gpu.performanceScore * 0.65 +
                cpu.performanceScore * 0.15 +
                ramScore * 0.10 +
                tgpScore * 0.10 +
                usageBoost.gameBoost,
        )

        val creatorScore = clampScore(
            cpu.performanceScore * 0.30 +
                (gpu.performanceScore + gpu.creatorBonus) * 0.25 +
                ramScore * 0.20 +
                displayScore * 0.15 +
                batteryCapacityScore * 0.10 +
                usageBoost.creatorBoost,
        )

        return ProfileScores(
            batteryTier = batteryTier(laptop.batteryCapacity),
            portabilityTier = portabilityTier(laptop.weight),
            officeScore = officeScore,
            batteryScore = batteryScore,
            casualGameScore = casualGameScore,
            onlineGameScore = onlineGameScore,
            aaaGameScore = aaaGameScore,
            creatorScore = creatorScore,
            portabilityScore = portabilityScore,
            displayScore = displayScore,
            ramScore = ramScore,
            tgpScore = tgpScore,
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

    fun displayScore(laptop: PersistedCrawledLaptopSnapshot): Int {
        val resolution = laptop.resolution.orEmpty()
        val resolutionMatch = RESOLUTION_REGEX.find(resolution)
        val pixelScore = if (resolutionMatch != null) {
            val width = resolutionMatch.groupValues[1].toInt()
            val height = resolutionMatch.groupValues[2].toInt()
            ((width.toDouble() * height) / REFERENCE_PIXELS * 100.0).coerceIn(20.0, 100.0)
        } else {
            50.0
        }

        val brightnessScore = when (val brightness = laptop.brightness) {
            null -> 55.0
            else -> (brightness.toDouble() / 500.0 * 100.0).coerceIn(35.0, 100.0)
        }

        val refreshScore = when (val refreshRate = laptop.refreshRate) {
            null -> 50.0
            else -> (refreshRate.toDouble() / 240.0 * 100.0).coerceIn(25.0, 100.0)
        }

        return clampScore((pixelScore * 0.60) + (brightnessScore * 0.25) + (refreshScore * 0.15))
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

    private fun usageSet(laptop: PersistedCrawledLaptopSnapshot): UsageBoosts {
        val usages = laptop.usages.map { it.trim() }.toSet()
        return UsageBoosts(
            officeBoost = if ("사무/인강용" in usages) 8 else 0,
            portableBoost = if ("휴대용" in usages) 8 else 0,
            creatorBoost = if ("그래픽작업용" in usages) 8 else 0,
            gameBoost = if ("게임용" in usages) 8 else 0,
        )
    }

    private fun clampScore(value: Double): Int {
        return value.roundToInt().coerceIn(0, 100)
    }

    private data class UsageBoosts(
        val officeBoost: Int,
        val portableBoost: Int,
        val creatorBoost: Int,
        val gameBoost: Int,
    )

    private companion object {
        private val RESOLUTION_REGEX = Regex("""([0-9]{3,4})x([0-9]{3,4})""", RegexOption.IGNORE_CASE)
        private const val REFERENCE_PIXELS = 2880.0 * 1800.0
    }
}
