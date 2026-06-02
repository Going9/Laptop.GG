package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.GpuClass

internal object GpuModelCatalog {
    fun classify(rawGpu: String, normalizedGpu: String): GpuInsights {
        return MODEL_RULES
            .firstOrNull { it.matches(normalizedGpu) }
            ?.toInsights(rawGpu)
            ?: GpuClassificationRule(
                tokens = listOf(UNKNOWN_TOKEN),
                performanceScore = 35,
                gpuClass = GpuClass.UNKNOWN,
                integrated = false,
            ).toInsights(rawGpu)
    }

    private fun rule(
        token: String,
        performanceScore: Int,
        gpuClass: GpuClass,
        creatorBonus: Int = 0,
        integrated: Boolean? = null,
    ): GpuClassificationRule {
        return GpuClassificationRule(
            tokens = listOf(token),
            performanceScore = performanceScore,
            gpuClass = gpuClass,
            creatorBonus = creatorBonus,
            integrated = integrated,
        )
    }

    private fun rule(
        tokens: List<String>,
        performanceScore: Int,
        gpuClass: GpuClass,
        creatorBonus: Int = 0,
        integrated: Boolean? = null,
    ): GpuClassificationRule {
        return GpuClassificationRule(
            tokens = tokens,
            performanceScore = performanceScore,
            gpuClass = gpuClass,
            creatorBonus = creatorBonus,
            integrated = integrated,
        )
    }

    private val MODEL_RULES = listOf(
        rule("RTX 5090", 100, GpuClass.DISCRETE_ENTHUSIAST),
        rule("RTX 5080", 96, GpuClass.DISCRETE_ENTHUSIAST),
        rule("RTX 5070 TI", 93, GpuClass.DISCRETE_HIGH),
        rule("RTX 5070", 90, GpuClass.DISCRETE_HIGH),
        rule("RTX 5060", 84, GpuClass.DISCRETE_HIGH),
        rule("RTX 5050", 72, GpuClass.DISCRETE_MAINSTREAM),
        rule("RTX 4090", 99, GpuClass.DISCRETE_ENTHUSIAST),
        rule("RTX 4080", 95, GpuClass.DISCRETE_ENTHUSIAST),
        rule("RTX 4070", 88, GpuClass.DISCRETE_HIGH),
        rule("RTX 4060", 80, GpuClass.DISCRETE_MAINSTREAM),
        rule("RTX 4050", 68, GpuClass.DISCRETE_MAINSTREAM),
        rule("RTX 3080 TI", 90, GpuClass.DISCRETE_HIGH),
        rule("RTX 3080", 88, GpuClass.DISCRETE_HIGH),
        rule("RTX 3070 TI", 84, GpuClass.DISCRETE_HIGH),
        rule("RTX 3070", 80, GpuClass.DISCRETE_MAINSTREAM),
        rule("RTX 3060", 72, GpuClass.DISCRETE_MAINSTREAM),
        rule("RTX 3050 TI", 58, GpuClass.DISCRETE_ENTRY),
        rule("RTX 3050", 52, GpuClass.DISCRETE_ENTRY),
        rule("RTX 2050", 35, GpuClass.DISCRETE_ENTRY),
        rule("RTX PRO 5000", 92, GpuClass.WORKSTATION, creatorBonus = 12, integrated = false),
        rule("RTX PRO 4000", 86, GpuClass.WORKSTATION, creatorBonus = 10, integrated = false),
        rule("RTX PRO 3000", 80, GpuClass.WORKSTATION, creatorBonus = 8, integrated = false),
        rule("RTX PRO 2000", 74, GpuClass.WORKSTATION, creatorBonus = 8, integrated = false),
        rule("RTX PRO 1000", 66, GpuClass.WORKSTATION, creatorBonus = 6, integrated = false),
        rule("RTX PRO 500", 55, GpuClass.WORKSTATION, creatorBonus = 5, integrated = false),
        rule("RTX A500", 45, GpuClass.DISCRETE_ENTRY),
        rule("RTX A1000", 58, GpuClass.WORKSTATION, creatorBonus = 6, integrated = false),
        rule("ARC B390", 78, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("ARC B370", 66, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("ARC 140T", 62, GpuClass.INTEGRATED_HIGH),
        rule("ARC 130T", 58, GpuClass.INTEGRATED_HIGH),
        rule("ARC", 55, GpuClass.INTEGRATED_HIGH),
        rule("RADEON RX 7600M XT", 80, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("RADEON RX 7600S", 72, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("RADEON RX 6800S", 78, GpuClass.DISCRETE_HIGH, integrated = false),
        rule("RADEON RX 6700S", 68, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("RADEON RX 6850M XT", 88, GpuClass.DISCRETE_HIGH, integrated = false),
        rule("RADEON RX 6700M", 74, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("RADEON RX 6650M", 66, GpuClass.DISCRETE_MAINSTREAM, integrated = false),
        rule("RADEON RX 6600M", 62, GpuClass.DISCRETE_ENTRY, integrated = false),
        rule("RADEON RX 6500M", 46, GpuClass.DISCRETE_ENTRY, integrated = false),
        rule("RADEON 890M", 68, GpuClass.INTEGRATED_HIGH),
        rule("RADEON 880M", 65, GpuClass.INTEGRATED_HIGH),
        rule("RADEON 860M", 60, GpuClass.INTEGRATED_HIGH),
        rule("RADEON 840M", 50, GpuClass.INTEGRATED_MAINSTREAM),
        rule("RADEON 820M", 40, GpuClass.INTEGRATED_MAINSTREAM),
        rule("RADEON 8060S", 76, GpuClass.INTEGRATED_HIGH),
        rule("RADEON 780M", 60, GpuClass.INTEGRATED_HIGH),
        rule("RADEON 760M", 50, GpuClass.INTEGRATED_MAINSTREAM),
        rule("RADEON 740M", 42, GpuClass.INTEGRATED_MAINSTREAM),
        rule("RADEON 680M", 56, GpuClass.INTEGRATED_HIGH),
        rule("RADEON 660M", 46, GpuClass.INTEGRATED_MAINSTREAM),
        rule("RADEON 610M", 26, GpuClass.INTEGRATED_ENTRY),
        rule("RADEON GRAPHICS", 38, GpuClass.INTEGRATED_MAINSTREAM),
        rule("INTEL GRAPHICS", 42, GpuClass.INTEGRATED_MAINSTREAM),
        rule("IRIS XE", 46, GpuClass.INTEGRATED_MAINSTREAM),
        rule("UHD GRAPHICS", 28, GpuClass.INTEGRATED_ENTRY),
        rule("ADRENO X2-90", 66, GpuClass.INTEGRATED_HIGH),
        rule(listOf("ADRENO", "4.6"), 60, GpuClass.INTEGRATED_HIGH),
        rule(listOf("ADRENO", "3.8"), 55, GpuClass.INTEGRATED_HIGH),
        rule(listOf("ADRENO", "1.7"), 42, GpuClass.INTEGRATED_MAINSTREAM),
        rule("ADRENO", 45, GpuClass.INTEGRATED_MAINSTREAM),
    )

    private const val UNKNOWN_TOKEN = "<UNKNOWN>"
}
