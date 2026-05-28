package going9.laptopgg.application.service

import going9.laptopgg.domain.laptop.GpuClass
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.stereotype.Component

data class GpuInsights(
    val normalizedGpu: String?,
    val gpuClass: GpuClass,
    val performanceScore: Int,
    val creatorBonus: Int,
    val isIntegrated: Boolean,
)

@Component
class GpuClassifier {
    fun classify(laptop: Laptop): GpuInsights {
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
}
