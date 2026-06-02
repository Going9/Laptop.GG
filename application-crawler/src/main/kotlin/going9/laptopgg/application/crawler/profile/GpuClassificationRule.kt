package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.GpuClass

internal data class GpuClassificationRule(
    val tokens: List<String>,
    val performanceScore: Int,
    val gpuClass: GpuClass,
    val creatorBonus: Int = 0,
    val integrated: Boolean? = null,
) {
    fun matches(normalizedGpu: String): Boolean {
        return tokens.all(normalizedGpu::contains)
    }

    fun toInsights(rawGpu: String): GpuInsights {
        return GpuInsights(
            normalizedGpu = rawGpu,
            gpuClass = gpuClass,
            performanceScore = performanceScore,
            creatorBonus = creatorBonus,
            isIntegrated = integrated ?: gpuClass.name.startsWith("INTEGRATED"),
        )
    }
}
