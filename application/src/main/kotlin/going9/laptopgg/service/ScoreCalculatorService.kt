package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.RecommendationUseCase
import org.springframework.stereotype.Service
import kotlin.math.round
import kotlin.math.roundToInt

@Service
class ScoreCalculatorService(
    private val recommendationScoringPolicy: RecommendationScoringPolicy,
) {
    data class ScoreResult(
        val score: Double,
        val reasons: List<String>,
    )

    fun calculateScore(
        laptop: Laptop,
        profile: LaptopProfile,
        request: LaptopRecommendationRequest,
    ): ScoreResult {
        val useCase = request.resolvedUseCase()
        val budgetScore = budgetScore(laptop.price, request.budget)
        val portabilityScore = profile.portabilityScore
        val displayScore = profile.displayScore
        val ramScore = profile.ramScore
        val tgpScore = profile.tgpScore
        val cpuPerformanceScore = profile.cpuPerformanceScore
        val lowPowerCpuScore = profile.lowPowerCpuScore
        val gpuScore = profile.gpuPerformanceScore
        val creatorGpuScore = (profile.gpuPerformanceScore + profile.gpuCreatorBonus).coerceAtMost(100)

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
                officeScore = profile.officeScore,
                batteryScore = profile.batteryScore,
            ),
        )

        return ScoreResult(
            score = round(rawScore * 10.0) / 10.0,
            reasons = buildReasons(
                useCase = useCase,
                budgetScore = budgetScore,
                portabilityScore = portabilityScore,
                displayScore = displayScore,
                ramScore = ramScore,
                tgpScore = tgpScore,
                cpuPerformanceScore = cpuPerformanceScore,
                lowPowerCpuScore = lowPowerCpuScore,
                gpuScore = gpuScore,
                officeScore = profile.officeScore,
                batteryScore = profile.batteryScore,
                casualGameScore = profile.casualGameScore,
                onlineGameScore = profile.onlineGameScore,
                aaaGameScore = profile.aaaGameScore,
                creatorScore = profile.creatorScore,
            ),
        )
    }

    fun gateScore(profile: LaptopProfile, useCase: RecommendationUseCase): Int {
        return recommendationScoringPolicy.gateScore(profile, useCase)
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

    private fun buildReasons(
        useCase: RecommendationUseCase,
        budgetScore: Int,
        portabilityScore: Int,
        displayScore: Int,
        ramScore: Int,
        tgpScore: Int,
        cpuPerformanceScore: Int,
        lowPowerCpuScore: Int,
        gpuScore: Int,
        officeScore: Int,
        batteryScore: Int,
        casualGameScore: Int,
        onlineGameScore: Int,
        aaaGameScore: Int,
        creatorScore: Int,
    ): List<String> {
        val balancedScore = ((officeScore + batteryScore + casualGameScore) / 3.0).roundToInt()
        val candidates = when (useCase) {
            RecommendationUseCase.NOT_SURE -> listOf(
                "일상용으로 두루 잘 맞아요" to balancedScore,
                "배터리가 오래가는 편이에요" to batteryScore,
                "들고 다니기 편한 편이에요" to portabilityScore,
                "예산에 잘 맞아요" to budgetScore,
                "문서 작업에 잘 맞아요" to officeScore,
            )
            RecommendationUseCase.OFFICE_STUDY -> listOf(
                "문서 작업에 잘 맞아요" to officeScore,
                "들고 다니기 편한 편이에요" to portabilityScore,
                "배터리가 오래가는 편이에요" to batteryScore,
                "화면이 보기 편해요" to displayScore,
                "예산에 잘 맞아요" to budgetScore,
            )
            RecommendationUseCase.PORTABLE_OFFICE -> listOf(
                "가볍게 들고 다니기 좋아요" to portabilityScore,
                "배터리가 오래가는 편이에요" to batteryScore,
                "업무용으로 무난해요" to officeScore,
                "화면이 보기 편해요" to displayScore,
                "예산에 잘 맞아요" to budgetScore,
            )
            RecommendationUseCase.BATTERY_FIRST -> listOf(
                "배터리가 오래가는 편이에요" to batteryScore,
                "전력 효율이 좋은 편이에요" to lowPowerCpuScore,
                "들고 다니기 편한 편이에요" to portabilityScore,
                "문서 작업에 잘 맞아요" to officeScore,
                "예산에 잘 맞아요" to budgetScore,
            )
            RecommendationUseCase.CASUAL_GAME -> listOf(
                "가벼운 게임에 잘 맞아요" to casualGameScore,
                "그래픽이 괜찮은 편이에요" to gpuScore,
                "메모리가 넉넉한 편이에요" to ramScore,
                "들고 다니기 무난해요" to portabilityScore,
                "화면이 보기 편해요" to displayScore,
            )
            RecommendationUseCase.ONLINE_GAME -> listOf(
                "온라인 게임에 잘 맞아요" to onlineGameScore,
                "그래픽이 좋은 편이에요" to gpuScore,
                "프로세서가 빠른 편이에요" to cpuPerformanceScore,
                "게임 성능 여유가 있어요" to tgpScore,
                "메모리가 넉넉한 편이에요" to ramScore,
            )
            RecommendationUseCase.AAA_GAME -> listOf(
                "고사양 게임에 잘 맞아요" to aaaGameScore,
                "그래픽이 좋은 편이에요" to gpuScore,
                "게임 성능 여유가 있어요" to tgpScore,
                "프로세서가 빠른 편이에요" to cpuPerformanceScore,
                "메모리가 넉넉한 편이에요" to ramScore,
            )
            RecommendationUseCase.CREATOR -> listOf(
                "사진·영상 작업에 잘 맞아요" to creatorScore,
                "프로세서가 빠른 편이에요" to cpuPerformanceScore,
                "그래픽이 좋은 편이에요" to gpuScore,
                "메모리가 넉넉한 편이에요" to ramScore,
                "화면이 보기 편해요" to displayScore,
            )
        }

        return candidates
            .sortedByDescending { it.second }
            .map { it.first }
            .take(2)
    }
}
