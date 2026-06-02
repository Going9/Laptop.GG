package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.LaptopDisplayTextPolicy
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
            manufacturer = LaptopDisplayTextPolicy.manufacturerName(candidate.name),
            weight = candidate.weight,
            screenSize = candidate.screenSize,
            cpu = candidate.cpu,
            gpu = candidate.graphicsType,
            resolutionLabel = LaptopDisplayTextPolicy.resolutionLabel(candidate.resolution),
            reasons = scoreResult.reasons,
        )
    }
}
