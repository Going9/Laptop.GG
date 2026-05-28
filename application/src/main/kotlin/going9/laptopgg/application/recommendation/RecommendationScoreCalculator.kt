package going9.laptopgg.application.recommendation

import going9.laptopgg.application.recommendation.port.RecommendationCandidateRecord
import going9.laptopgg.recommendation.RecommendationScoreInputs
import going9.laptopgg.recommendation.RecommendationScoringPolicy
import going9.laptopgg.recommendation.RecommendationUseCase
import kotlin.math.round
import kotlin.math.roundToInt

class RecommendationScoreCalculator(
    private val recommendationReasonBuilder: RecommendationReasonBuilder,
    private val recommendationScoringPolicy: RecommendationScoringPolicy = RecommendationScoringPolicy(),
) {
    data class ScoreResult(
        val score: Double,
        val reasons: List<String>,
    )

    fun calculateScore(
        candidate: RecommendationCandidateRecord,
        request: LaptopRecommendationQuery,
    ): ScoreResult {
        val useCase = request.resolvedUseCase()
        val budgetScore = budgetScore(candidate.price, request.budget)
        val portabilityScore = candidate.portabilityScore
        val displayScore = candidate.displayScore
        val ramScore = candidate.ramScore
        val tgpScore = candidate.tgpScore
        val cpuPerformanceScore = candidate.cpuPerformanceScore
        val lowPowerCpuScore = candidate.lowPowerCpuScore
        val gpuScore = candidate.gpuPerformanceScore
        val creatorGpuScore = (candidate.gpuPerformanceScore + candidate.gpuCreatorBonus).coerceAtMost(100)

        val rawScore = recommendationScoringPolicy.weightedScore(
            useCase,
            RecommendationScoreInputs(
                budgetScore = budgetScore,
                portabilityScore = portabilityScore,
                displayScore = displayScore,
                ramScore = ramScore,
                tgpScore = tgpScore,
                cpuPerformanceScore = cpuPerformanceScore,
                lowPowerCpuScore = lowPowerCpuScore,
                gpuScore = gpuScore,
                creatorGpuScore = creatorGpuScore,
                officeScore = candidate.officeScore,
                batteryScore = candidate.batteryScore,
            ),
        )

        return ScoreResult(
            score = round(rawScore * 10.0) / 10.0,
            reasons = recommendationReasonBuilder.build(
                useCase = useCase,
                candidate = candidate,
                budgetScore = budgetScore,
            ),
        )
    }

    fun gateThreshold(useCase: RecommendationUseCase): Int {
        return recommendationScoringPolicy.gateThreshold(useCase)
    }

    private fun budgetScore(price: Int?, budget: Int): Int {
        if (price == null || budget <= 0) {
            return 0
        }

        if (price > budget) {
            return 0
        }

        val savingsRatio = (1.0 - (price.toDouble() / budget.toDouble())).coerceIn(0.0, 1.0)
        return (60.0 + (savingsRatio * 40.0)).roundToInt().coerceIn(0, 100)
    }
}
