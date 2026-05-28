package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.RecommendationScorePort
import going9.laptopgg.recommendation.RecommendationGateInputs
import going9.laptopgg.recommendation.RecommendationScoreInputs
import going9.laptopgg.recommendation.RecommendationScoringPolicy
import going9.laptopgg.recommendation.RecommendationUseCase
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@Transactional
class RecommendationScoreService(
    private val recommendationScorePort: RecommendationScorePort,
) {
    private val recommendationScoringPolicy = RecommendationScoringPolicy()

    fun refreshScores(profileState: CrawledLaptopProfileState) {
        val profile = profileState.profile
        val inputs = scoreInputs(profile)
        val gateInputs = gateInputs(profile)
        val now = LocalDateTime.now()

        val scores = RecommendationUseCase.entries.map { useCase ->
            UpsertRecommendationScoreCommand(
                laptopId = profileState.laptopId,
                useCase = useCase.name,
                gateScore = recommendationScoringPolicy.gateScore(gateInputs, useCase),
                staticScore = recommendationScoringPolicy.staticScore(useCase, inputs),
                budgetWeight = recommendationScoringPolicy.budgetWeight(useCase),
                updatedAt = now,
            )
        }

        recommendationScorePort.saveAll(scores)
    }

    private fun scoreInputs(profile: LaptopProfileSnapshot): RecommendationScoreInputs {
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

    private fun gateInputs(profile: LaptopProfileSnapshot): RecommendationGateInputs {
        return RecommendationGateInputs(
            officeScore = profile.officeScore,
            batteryScore = profile.batteryScore,
            casualGameScore = profile.casualGameScore,
            onlineGameScore = profile.onlineGameScore,
            aaaGameScore = profile.aaaGameScore,
            creatorScore = profile.creatorScore,
        )
    }
}
