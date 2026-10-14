package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.CpuClass
import kotlin.math.roundToInt

internal data class CpuInsights(
    val normalizedCpu: String?,
    val cpuClass: CpuClass,
    val performanceScore: Int,
    val lowPowerScore: Int,
)

internal class CpuClassifier(
    private val cpuTokenResolver: CpuTokenResolver = CpuTokenResolver(),
) {
    fun classify(laptop: LaptopProfileSource): CpuInsights {
        val resolvedCpu = cpuTokenResolver.resolve(laptop.cpu, laptop.cpuManufacturer, laptop.name)
        val normalized = cpuTokenResolver.normalize(resolvedCpu)

        if (normalized.isBlank()) {
            return CpuInsights(
                normalizedCpu = resolvedCpu,
                cpuClass = CpuClass.UNKNOWN,
                performanceScore = 50,
                lowPowerScore = 50,
            )
        }

        val appleCpu = cpuTokenResolver.findAppleCpuToken(normalized)
        if (appleCpu != null) {
            val suffix = appleCpu.suffix
            val suffixBonus = when {
                suffix.contains("ULTRA") -> 18
                suffix.contains("MAX") -> 12
                suffix.contains("PRO") -> 8
                else -> 0
            }
            val performance = clampScore(60.0 + (appleCpu.generation * 6.0) + suffixBonus.toDouble())
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

        val amd = laptop.cpuManufacturer.orEmpty().contains("AMD", ignoreCase = true) || normalized.contains("RYZEN")
        val model = MODEL_REGEX.find(normalized)
        if (amd && model != null) {
            val number = model.groupValues[1]
            val suffix = model.groupValues[2]
            // AI HX 370 is a 28W mobile part, unlike the high-power 7945HX family.
            val modelScores = AMD_MODEL_SCORES[number]
            if (modelScores != null && suffix.isBlank()) {
                val cpuClass = if (number in AMD_MAX_MODELS) CpuClass.PERFORMANCE else CpuClass.BALANCED
                return CpuInsights(resolvedCpu, cpuClass, modelScores.first, modelScores.second)
            }
            if (number.length == 4) {
                val architectureScore = when {
                    number[0] in '7'..'9' -> when (number[2]) {
                        '2' -> 42
                        '3' -> if (number[3] == '5') 64 else 58
                        '4' -> 74
                        '5' -> 80
                        else -> 60
                    }
                    number[0] == '6' -> 64
                    number[0] == '5' -> if (number[1] in "468") 58 else 52
                    number[0] == '4' -> 48
                    else -> 38
                }
                val tierBonus = when (number[1]) { '9' -> 12; '8', '7' -> 6; '6', '5' -> 0; else -> -8 }
                return seriesInsights(resolvedCpu, suffix, architectureScore + tierBonus)
            }
        }

        val intelCore = INTEL_CORE_REGEX.find(normalized)
        if (!amd && intelCore != null) {
            val number = intelCore.groupValues[2]
            val generation = number.take(if (number.startsWith("1")) 2 else 1).toInt()
            val tier = intelCore.groupValues[1].toInt()
            val base = 44 + (generation.coerceIn(6, 14) - 6) * 3 + (tier - 5) * 3
            return seriesInsights(resolvedCpu, intelCore.groupValues[3], base)
        }
        if (!amd && model != null && model.groupValues[1].length == 3 && model.groupValues[2].isNotBlank()) {
            val number = model.groupValues[1]
            val base = 64 + (number[0].digitToInt().coerceIn(1, 3) - 1) * 4 +
                when { number[1] >= '8' -> 12; number[1] >= '5' -> 6; else -> 0 }
            return seriesInsights(resolvedCpu, model.groupValues[2], base)
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

        return CpuInsights(
            normalizedCpu = resolvedCpu,
            cpuClass = CpuClass.BALANCED,
            performanceScore = 72,
            lowPowerScore = 65,
        )
    }

    private fun seriesInsights(cpu: String?, suffix: String, base: Int): CpuInsights {
        val (cpuClass, performanceBonus, efficiency) = when (suffix) {
            "HX3D" -> Triple(CpuClass.WORKSTATION, 22, 18)
            "HX" -> Triple(CpuClass.ENTHUSIAST, 18, 22)
            "HK", "H", "HQ" -> Triple(CpuClass.PERFORMANCE, 10, 40)
            "HS" -> Triple(CpuClass.PERFORMANCE, 8, 55)
            "P" -> Triple(CpuClass.BALANCED, 4, 65)
            "U", "G7", "G4", "G1" -> Triple(CpuClass.LOW_POWER, 0, 84)
            "V" -> Triple(CpuClass.ULTRA_LOW_POWER, -2, 92)
            else -> Triple(CpuClass.BALANCED, 0, 65)
        }
        return CpuInsights(cpu, cpuClass, (base + performanceBonus).coerceIn(0, 99), efficiency)
    }

    private fun clampScore(value: Double): Int {
        return value.roundToInt().coerceIn(0, 100)
    }

    private companion object {
        // shortcut: spec-based relative scores, replace with measured benchmarks when a licensed dataset is available.
        private val AMD_MAX_MODELS = setOf("385", "388", "390", "392", "395")
        private val AMD_MODEL_SCORES = mapOf(
            "110" to (50 to 84), "130" to (64 to 84), "150" to (72 to 55),
            "160" to (70 to 84), "170" to (78 to 55),
            "210" to (62 to 84), "215" to (62 to 84), "220" to (72 to 84),
            "230" to (74 to 84), "240" to (82 to 55), "250" to (80 to 84),
            "260" to (88 to 55), "270" to (90 to 55),
            "330" to (68 to 86), "340" to (76 to 84), "350" to (82 to 82),
            "360" to (82 to 82), "365" to (88 to 78), "370" to (92 to 76),
            "375" to (92 to 76), "385" to (86 to 50), "388" to (86 to 50),
            "390" to (94 to 50), "392" to (94 to 50), "395" to (99 to 50),
            "430" to (70 to 86), "435" to (78 to 84), "440" to (78 to 84),
            "445" to (78 to 84), "450" to (84 to 82), "460" to (84 to 82),
            "465" to (90 to 78), "470" to (94 to 76), "475" to (94 to 76),
        )
        private val MODEL_REGEX = Regex("""\b(\d{3,5})(HX3D|HX|HS|HK|HQ|H|U|V|P|G[147])?\b""")
        private val INTEL_CORE_REGEX = Regex("""\bI([3579])[- ](\d{4,5})(HX|HK|HQ|H|U|P|G[147])\b""")
        private val H_SERIES_REGEX = Regex("""(?:^|[^A-Z])(?:\d{3,5}|I[3579]-\d{4,5})H(?:X)?(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val U_SERIES_REGEX = Regex("""(?:^|[^A-Z])(?:\d{3,5}|I[3579]-\d{4,5})U(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
        private val V_SERIES_REGEX = Regex("""(?:^|[^A-Z])\d{3,5}V(?:[^A-Z]|$)""", RegexOption.IGNORE_CASE)
    }
}
