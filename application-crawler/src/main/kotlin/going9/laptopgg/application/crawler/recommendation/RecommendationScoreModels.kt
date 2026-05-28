package going9.laptopgg.application.crawler.recommendation

import java.time.LocalDateTime

data class UpsertRecommendationScoreCommand(
    val laptopId: Long,
    val useCase: String,
    val gateScore: Int,
    val staticScore: Double,
    val budgetWeight: Double,
    val updatedAt: LocalDateTime,
)
