package going9.laptopgg.application.crawler.recommendation

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.recommendation.RecommendationGateInputs
import going9.laptopgg.recommendation.RecommendationScoreInputs
import going9.laptopgg.recommendation.RecommendationScoringPolicy
import going9.laptopgg.recommendation.RecommendationUseCase
import java.time.LocalDateTime

class RecommendationScoreService(
    private val recommendationScorePort: RecommendationScorePort,
    private val transactionPort: CrawlerTransactionPort,
) {
    private val recommendationScoringPolicy = RecommendationScoringPolicy()

    fun refreshScores(profileState: CrawledLaptopProfileState) {
        transactionPort.write {
            refreshScoresInTransaction(profileState)
        }
    }

    private fun refreshScoresInTransaction(profileState: CrawledLaptopProfileState) {
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
