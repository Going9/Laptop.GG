package going9.laptopgg.service

import going9.laptopgg.domain.laptop.BatteryTier
import going9.laptopgg.domain.laptop.CpuClass
import going9.laptopgg.domain.laptop.GpuClass
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.PortabilityTier
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

@Component
class LaptopProfileFactory {
    data class Snapshot(
        val cpuClass: CpuClass,
        val gpuClass: GpuClass,
        val batteryTier: BatteryTier,
        val portabilityTier: PortabilityTier,
        val officeScore: Int,
        val batteryScore: Int,
        val casualGameScore: Int,
        val onlineGameScore: Int,
        val aaaGameScore: Int,
        val creatorScore: Int,
    )

    data class CpuInsights(
        val normalizedCpu: String?,
        val cpuClass: CpuClass,
        val performanceScore: Int,
        val lowPowerScore: Int,
    )

    data class GpuInsights(
        val normalizedGpu: String?,
        val gpuClass: GpuClass,
        val performanceScore: Int,
        val creatorBonus: Int,
        val isIntegrated: Boolean,
    )

    fun build(laptop: Laptop): Snapshot {
        val cpu = resolveCpuInsights(laptop)
        val gpu = resolveGpuInsights(laptop)
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

        return Snapshot(
            cpuClass = cpu.cpuClass,
            gpuClass = gpu.gpuClass,
            batteryTier = batteryTier(laptop.batteryCapacity),
            portabilityTier = portabilityTier(laptop.weight),
            officeScore = officeScore,
            batteryScore = batteryScore,
            casualGameScore = casualGameScore,
            onlineGameScore = onlineGameScore,
            aaaGameScore = aaaGameScore,
            creatorScore = creatorScore,
        )
    }

    fun resolveCpuInsights(laptop: Laptop): CpuInsights {
        val resolvedCpu = resolveCpuToken(laptop.cpu, laptop.cpuManufacturer, laptop.name)
        val normalized = normalizeCpuToken(resolvedCpu)

        if (normalized.isBlank()) {
            return CpuInsights(
                normalizedCpu = resolvedCpu,
                cpuClass = CpuClass.UNKNOWN,
                performanceScore = 50,
                lowPowerScore = 50,
            )
        }

        val appleMatch = APPLE_CPU_REGEX.find(normalized)
        if (appleMatch != null) {
            val generation = appleMatch.groupValues[1].toIntOrNull() ?: 1
            val suffix = appleMatch.groupValues[2]
            val suffixBonus = when {
                suffix.contains("ULTRA") -> 18
                suffix.contains("MAX") -> 12
                suffix.contains("PRO") -> 8
                else -> 0
            }
            val performance = clampScore(60.0 + (generation * 6.0) + suffixBonus.toDouble())
            val lowPower = when {
                suffix.contains("ULTRA") -> 62
                suffix.contains("MAX") -> 68
                suffix.contains("PRO") -> 76
                else -> 90
            }

            return CpuInsights(
                normalizedCpu = resolvedCpu,
                cpuClass = when {
                    suffix.contains("ULTRA") -> CpuClass.WORKSTATION
                    suffix.contains("MAX") -> CpuClass.ENTHUSIAST
                    suffix.contains("PRO") -> CpuClass.PERFORMANCE
                    else -> CpuClass.BALANCED
                },
                performanceScore = performance,
                lowPowerScore = lowPower,
            )
        }

        if (normalized.contains("X2E-") || normalized.contains("X ELITE") || normalized.contains("X1E-")) {
            val performance = when {
                normalized.contains("X2E-") -> 88
                normalized.contains("X ELITE") -> 84
                else -> 80
            }
            return CpuInsights(resolvedCpu, CpuClass.BALANCED, performance, 90)
        }

        if (normalized.contains("X PLUS") || normalized.contains("X1P-") || normalized.contains("X1-")) {
            return CpuInsights(resolvedCpu, CpuClass.LOW_POWER, 74, 92)
        }

        if (normalized == "N100") return CpuInsights(resolvedCpu, CpuClass.ULTRA_LOW_POWER, 25, 78)
        if (normalized == "N200") return CpuInsights(resolvedCpu, CpuClass.ULTRA_LOW_POWER, 30, 78)
        if (normalized.contains("I3-N305")) return CpuInsights(resolvedCpu, CpuClass.ULTRA_LOW_POWER, 36, 75)

        if (normalized.contains("HX3D")) return CpuInsights(resolvedCpu, CpuClass.WORKSTATION, 99, 18)
        if (normalized.contains("HX")) return CpuInsights(resolvedCpu, CpuClass.ENTHUSIAST, 94, 22)
        if (normalized.contains("HS")) return CpuInsights(resolvedCpu, CpuClass.PERFORMANCE, 84, 45)
        if (H_SERIES_REGEX.containsMatchIn(normalized)) return CpuInsights(resolvedCpu, CpuClass.PERFORMANCE, 82, 40)
        if (U_SERIES_REGEX.containsMatchIn(normalized)) return CpuInsights(resolvedCpu, CpuClass.LOW_POWER, 68, 84)
        if (V_SERIES_REGEX.containsMatchIn(normalized)) return CpuInsights(resolvedCpu, CpuClass.ULTRA_LOW_POWER, 72, 88)

        if (normalized == "340" || normalized == "350" || normalized == "PRO 340" || normalized == "PRO 350") {
            return CpuInsights(resolvedCpu, CpuClass.BALANCED, 78, 84)
        }

        if (normalized == "365" || normalized == "370" || normalized == "375" || normalized == "385" || normalized == "435") {
            return CpuInsights(resolvedCpu, CpuClass.BALANCED, 82, 82)
        }

        return CpuInsights(
            normalizedCpu = resolvedCpu,
            cpuClass = CpuClass.BALANCED,
            performanceScore = 72,
            lowPowerScore = 65,
        )
    }

    fun resolveGpuInsights(laptop: Laptop): GpuInsights {
        val rawGpu = laptop.graphicsType?.trim().takeUnless { it.isNullOrBlank() } ?: return GpuInsights(
            normalizedGpu = null,
            gpuClass = GpuClass.UNKNOWN,
            performanceScore = 20,
            creatorBonus = 0,
            isIntegrated = true,
        )
        val normalized = normalizeGpuToken(rawGpu)

        fun result(
            score: Int,
            gpuClass: GpuClass,
            creatorBonus: Int = 0,
            integrated: Boolean = gpuClass.name.startsWith("INTEGRATED"),
        ) = GpuInsights(
            normalizedGpu = rawGpu,
            gpuClass = gpuClass,
            performanceScore = score,
            creatorBonus = creatorBonus,
            isIntegrated = integrated,
        )

        return when {
            normalized.contains("RTX 5090") -> result(100, GpuClass.DISCRETE_ENTHUSIAST)
            normalized.contains("RTX 5080") -> result(96, GpuClass.DISCRETE_ENTHUSIAST)
            normalized.contains("RTX 5070 TI") -> result(93, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 5070") -> result(90, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 5060") -> result(84, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 5050") -> result(72, GpuClass.DISCRETE_MAINSTREAM)
            normalized.contains("RTX 4090") -> result(99, GpuClass.DISCRETE_ENTHUSIAST)
            normalized.contains("RTX 4080") -> result(95, GpuClass.DISCRETE_ENTHUSIAST)
            normalized.contains("RTX 4070") -> result(88, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 4060") -> result(80, GpuClass.DISCRETE_MAINSTREAM)
            normalized.contains("RTX 4050") -> result(68, GpuClass.DISCRETE_MAINSTREAM)
            normalized.contains("RTX 3080 TI") -> result(90, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 3080") -> result(88, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 3070 TI") -> result(84, GpuClass.DISCRETE_HIGH)
            normalized.contains("RTX 3070") -> result(80, GpuClass.DISCRETE_MAINSTREAM)
            normalized.contains("RTX 3060") -> result(72, GpuClass.DISCRETE_MAINSTREAM)
            normalized.contains("RTX 3050 TI") -> result(58, GpuClass.DISCRETE_ENTRY)
            normalized.contains("RTX 3050") -> result(52, GpuClass.DISCRETE_ENTRY)
            normalized.contains("RTX 2050") -> result(35, GpuClass.DISCRETE_ENTRY)
            normalized.contains("RTX PRO 5000") -> result(92, GpuClass.WORKSTATION, creatorBonus = 12, integrated = false)
            normalized.contains("RTX PRO 4000") -> result(86, GpuClass.WORKSTATION, creatorBonus = 10, integrated = false)
            normalized.contains("RTX PRO 3000") -> result(80, GpuClass.WORKSTATION, creatorBonus = 8, integrated = false)
            normalized.contains("RTX PRO 2000") -> result(74, GpuClass.WORKSTATION, creatorBonus = 8, integrated = false)
            normalized.contains("RTX PRO 1000") -> result(66, GpuClass.WORKSTATION, creatorBonus = 6, integrated = false)
            normalized.contains("RTX PRO 500") -> result(55, GpuClass.WORKSTATION, creatorBonus = 5, integrated = false)
            normalized.contains("RTX A500") -> result(45, GpuClass.DISCRETE_ENTRY)
            normalized.contains("RTX A1000") -> result(58, GpuClass.WORKSTATION, creatorBonus = 6, integrated = false)
            normalized.contains("ARC B390") -> result(78, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("ARC B370") -> result(66, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("ARC 140T") -> result(62, GpuClass.INTEGRATED_HIGH)
            normalized.contains("ARC 130T") -> result(58, GpuClass.INTEGRATED_HIGH)
            normalized.contains("ARC B370") -> result(66, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("ARC") -> result(55, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON RX 7600M XT") -> result(80, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("RADEON RX 7600S") -> result(72, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("RADEON RX 6800S") -> result(78, GpuClass.DISCRETE_HIGH, integrated = false)
            normalized.contains("RADEON RX 6700S") -> result(68, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("RADEON RX 6850M XT") -> result(88, GpuClass.DISCRETE_HIGH, integrated = false)
            normalized.contains("RADEON RX 6700M") -> result(74, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("RADEON RX 6650M") -> result(66, GpuClass.DISCRETE_MAINSTREAM, integrated = false)
            normalized.contains("RADEON RX 6600M") -> result(62, GpuClass.DISCRETE_ENTRY, integrated = false)
            normalized.contains("RADEON RX 6500M") -> result(46, GpuClass.DISCRETE_ENTRY, integrated = false)
            normalized.contains("RADEON 890M") -> result(68, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON 880M") -> result(65, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON 860M") -> result(60, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON 840M") -> result(50, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("RADEON 820M") -> result(40, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("RADEON 8060S") -> result(76, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON 780M") -> result(60, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON 760M") -> result(50, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("RADEON 740M") -> result(42, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("RADEON 680M") -> result(56, GpuClass.INTEGRATED_HIGH)
            normalized.contains("RADEON 660M") -> result(46, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("RADEON 610M") -> result(26, GpuClass.INTEGRATED_ENTRY)
            normalized.contains("RADEON GRAPHICS") -> result(38, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("INTEL GRAPHICS") -> result(42, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("IRIS XE") -> result(46, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("UHD GRAPHICS") -> result(28, GpuClass.INTEGRATED_ENTRY)
            normalized.contains("ADRENO X2-90") -> result(66, GpuClass.INTEGRATED_HIGH)
            normalized.contains("ADRENO") && normalized.contains("4.6") -> result(60, GpuClass.INTEGRATED_HIGH)
            normalized.contains("ADRENO") && normalized.contains("3.8") -> result(55, GpuClass.INTEGRATED_HIGH)
            normalized.contains("ADRENO") && normalized.contains("1.7") -> result(42, GpuClass.INTEGRATED_MAINSTREAM)
            normalized.contains("ADRENO") -> result(45, GpuClass.INTEGRATED_MAINSTREAM)
            else -> result(35, GpuClass.UNKNOWN)
        }
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

    fun displayScore(laptop: Laptop): Int {
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

    fun resolveCpuToken(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        rawCpu?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val normalizedName = productName.uppercase()
            .replace("프로", " PRO")
            .replace("맥스", " MAX")
            .replace("울트라", " ULTRA")

        if (cpuManufacturer.orEmpty().contains("애플") || cpuManufacturer.orEmpty().contains("APPLE", ignoreCase = true) ||
            normalizedName.contains("MACBOOK") || normalizedName.contains("맥북")
        ) {
            val match = APPLE_CPU_REGEX.find(normalizedName)
            if (match != null) {
                val suffix = match.groupValues[2].trim()
                return listOf("M${match.groupValues[1]}", suffix)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim()
            }
        }

        return when {
            normalizedName.contains("X ELITE") -> "X Elite"
            normalizedName.contains("X PLUS") -> "X Plus"
            else -> null
        }
    }

    fun normalizeCpuToken(cpu: String?): String {
        return cpu.orEmpty()
            .uppercase()
            .replace("프로", " PRO")
            .replace("맥스", " MAX")
            .replace("울트라", " ULTRA")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun normalizeGpuToken(gpu: String): String {
        return gpu.uppercase()
            .replace(Regex("""RTX\s*PRO(?=\d)"""), "RTX PRO ")
            .replace(Regex("""RTX(?=\d)"""), "RTX ")
            .replace(Regex("""ARC(?=\d|B\d)"""), "ARC ")
            .replace(Regex("""RADEON(?=\d)"""), "RADEON ")
            .replace(Regex("""ADRENO(?=X?\d)"""), "ADRENO ")
            .replace(Regex("""(?<=\d)TI\b"""), " TI")
            .replace(Regex("""(?<=\d)(SUPER|MAX-Q)\b"""), " $1")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun usageSet(laptop: Laptop): UsageBoosts {
        val usages = laptop.laptopUsage.map { it.usage.trim() }.toSet()
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

    companion object {
        private val APPLE_CPU_REGEX = Regex("""M([1-9])(?:\s*(PRO|MAX|ULTRA))?""", RegexOption.IGNORE_CASE)
        private val H_SERIES_REGEX = Regex("""(?:^|[^A-Z])(?:\d{3,5}|I[3579]-\d{4,5})H(?:X)?(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val U_SERIES_REGEX = Regex("""(?:^|[^A-Z])(?:\d{3,5}|I[3579]-\d{4,5})U(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val V_SERIES_REGEX = Regex("""(?:^|[^A-Z])\d{3,5}V(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val RESOLUTION_REGEX = Regex("""([0-9]{3,4})x([0-9]{3,4})""", RegexOption.IGNORE_CASE)
        private const val REFERENCE_PIXELS = 2880.0 * 1800.0
    }
}
