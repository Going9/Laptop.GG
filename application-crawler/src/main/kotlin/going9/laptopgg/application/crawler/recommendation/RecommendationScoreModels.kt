package going9.laptopgg.application.crawler.recommendation

import going9.laptopgg.recommendation.RecommendationUseCase
import java.time.LocalDateTime

data class UpsertRecommendationScoreCommand(
    val laptopId: Long,
    val useCase: RecommendationUseCase,
    val gateScore: Int,
    val staticScore: Double,
    val budgetWeight: Double,
    val updatedAt: LocalDateTime,
)
