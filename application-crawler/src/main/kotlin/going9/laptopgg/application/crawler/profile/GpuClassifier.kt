package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.taxonomy.GpuClass

internal data class GpuInsights(
    val normalizedGpu: String?,
    val gpuClass: GpuClass,
    val performanceScore: Int,
    val creatorBonus: Int,
    val isIntegrated: Boolean,
)

internal class GpuClassifier {
    fun classify(laptop: PersistedCrawledLaptopSnapshot): GpuInsights {
        return classifyGraphics(laptop.graphicsType)
    }

    fun classifyGraphics(graphicsType: String?): GpuInsights {
        val rawGpu = graphicsType?.trim().takeUnless { it.isNullOrBlank() } ?: return GpuInsights(
            normalizedGpu = null,
            gpuClass = GpuClass.UNKNOWN,
            performanceScore = 20,
            creatorBonus = 0,
            isIntegrated = true,
        )
        val normalized = normalizeGpuToken(rawGpu)
        return GpuModelCatalog.classify(rawGpu, normalized)
    }

    fun isIntegratedGraphics(graphicsKind: String?, graphicsModel: String?): Boolean {
        val normalizedKind = graphicsKind.orEmpty().uppercase()
        val normalizedModel = graphicsModel.orEmpty().uppercase()

        if (normalizedKind.contains("외장")) {
            return false
        }
        if (normalizedKind.contains("내장")) {
            return true
        }

        if (GpuModelKeywordCatalog.isDiscreteModel(normalizedModel)) {
            return false
        }
        if (GpuModelKeywordCatalog.isIntegratedModel(normalizedModel)) {
            return true
        }

        return false
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
