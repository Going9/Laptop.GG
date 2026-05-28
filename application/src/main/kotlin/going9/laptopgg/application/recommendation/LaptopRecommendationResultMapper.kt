package going9.laptopgg.application.recommendation

import going9.laptopgg.application.recommendation.port.RecommendationCandidateRecord

internal class LaptopRecommendationResultMapper {
    fun toResult(
        candidate: RecommendationCandidateRecord,
        scoreResult: RecommendationScoreCalculator.ScoreResult,
    ): LaptopRecommendationResult {
        return LaptopRecommendationResult(
            id = candidate.id,
            score = scoreResult.score,
            imgLink = candidate.imageUrl,
            price = candidate.price,
            name = candidate.name,
            manufacturer = manufacturerName(candidate.name),
            weight = candidate.weight,
            screenSize = candidate.screenSize,
            cpu = candidate.cpu,
            gpu = candidate.graphicsType,
            resolutionLabel = resolutionLabel(candidate.resolution),
            reasons = scoreResult.reasons,
        )
    }

    private fun manufacturerName(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "브랜드 확인 불가"
    }

    private fun resolutionLabel(resolution: String?): String? {
        val raw = resolution?.trim().orEmpty()
        if (raw.isBlank()) {
            return null
        }

        val normalized = raw.uppercase()
        return when {
            normalized.contains("UHD") || normalized.contains("4K") -> "UHD"
            normalized.contains("QHD") || normalized.contains("WQHD") || normalized.contains("WQXGA") -> "QHD"
            normalized.contains("FHD") || normalized.contains("WUXGA") -> "FHD"
            normalized.contains("HD") -> "HD"
            else -> {
                val match = RESOLUTION_REGEX.find(normalized) ?: return raw
                val width = match.groupValues[1].toIntOrNull() ?: return raw
                when {
                    width >= 3840 -> "UHD"
                    width >= 2560 -> "QHD"
                    width >= 1920 -> "FHD"
                    width >= 1280 -> "HD"
                    else -> raw
                }
            }
        }
    }

    private companion object {
        private val RESOLUTION_REGEX = Regex("""(\d{3,4})\s*[xX]\s*(\d{3,4})""")
    }
}
