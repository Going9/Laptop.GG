package going9.laptopgg.service

import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.domain.recommendation.RecommendationScore
import going9.laptopgg.domain.repository.RecommendationScoreRepository
import going9.laptopgg.dto.request.RecommendationUseCase
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationScoreService(
    private val recommendationScoreRepository: RecommendationScoreRepository,
    private val recommendationScoringPolicy: RecommendationScoringPolicy,
) {
    @Transactional
    fun refreshScores(profile: LaptopProfile) {
        val laptopId = requireNotNull(profile.laptop.id) {
            "Laptop must be persisted before refreshing recommendation scores."
        }
        val existingScores = recommendationScoreRepository.findAllByLaptopId(laptopId)
            .associateBy { it.useCase }
        val inputs = scoreInputs(profile)
        val now = LocalDateTime.now()

        val scores = RecommendationUseCase.entries.map { useCase ->
            val useCaseName = useCase.name
            val score = existingScores[useCaseName] ?: RecommendationScore(
                laptop = profile.laptop,
                useCase = useCaseName,
                gateScore = 0,
                staticScore = 0.0,
                budgetWeight = 0.0,
            )
            score.gateScore = recommendationScoringPolicy.gateScore(profile, useCase)
            score.staticScore = recommendationScoringPolicy.staticScore(useCase, inputs)
            score.budgetWeight = recommendationScoringPolicy.budgetWeight(useCase)
            score.updatedAt = now
            score
        }

        recommendationScoreRepository.saveAll(scores)
    }

    private fun scoreInputs(profile: LaptopProfile): RecommendationScoreInputs {
        return RecommendationScoreInputs(
            budgetScore = 0,
            portabilityScore = profile.portabilityScore,
            displayScore = profile.displayScore,
            ramScore = profile.ramScore,
            tgpScore = profile.tgpScore,
            cpuPerformanceScore = profile.cpuPerformanceScore,
            lowPowerCpuScore = profile.lowPowerCpuScore,
            gpuScore = profile.gpuPerformanceScore,
            creatorGpuScore = (profile.gpuPerformanceScore + profile.gpuCreatorBonus).coerceAtMost(100),
            officeScore = profile.officeScore,
            batteryScore = profile.batteryScore,
        )
    }
}
