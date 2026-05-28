package going9.laptopgg.application.crawler

import going9.laptopgg.taxonomy.CpuClass
import kotlin.math.roundToInt

data class CpuInsights(
    val normalizedCpu: String?,
    val cpuClass: CpuClass,
    val performanceScore: Int,
    val lowPowerScore: Int,
)

class CpuClassifier {
    fun classify(laptop: PersistedCrawledLaptopSnapshot): CpuInsights {
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

    private fun clampScore(value: Double): Int {
        return value.roundToInt().coerceIn(0, 100)
    }

    private companion object {
        private val APPLE_CPU_REGEX = Regex("""M([1-9])(?:\s*(PRO|MAX|ULTRA))?""", RegexOption.IGNORE_CASE)
        private val H_SERIES_REGEX = Regex("""(?:^|[^A-Z])(?:\d{3,5}|I[3579]-\d{4,5})H(?:X)?(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val U_SERIES_REGEX = Regex("""(?:^|[^A-Z])(?:\d{3,5}|I[3579]-\d{4,5})U(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val V_SERIES_REGEX = Regex("""(?:^|[^A-Z])\d{3,5}V(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
    }
}
