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
    private val laptopProfileFactory: LaptopProfileFactory,
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
        val cpuInsights = laptopProfileFactory.resolveCpuInsights(laptop)
        val gpuInsights = laptopProfileFactory.resolveGpuInsights(laptop)
        val budgetScore = budgetScore(laptop.price, request.budget)
        val portabilityScore = laptopProfileFactory.portabilityScore(laptop.weight)
        val displayScore = laptopProfileFactory.displayScore(laptop)
        val ramScore = laptopProfileFactory.ramScore(laptop.ramSize)
        val tgpScore = laptopProfileFactory.tgpScore(laptop.tgp, gpuInsights.isIntegrated)

        val rawScore = when (useCase) {
            RecommendationUseCase.NOT_SURE -> {
                (profile.officeScore * 0.24) +
                    (profile.batteryScore * 0.20) +
                    (portabilityScore * 0.16) +
                    (budgetScore * 0.14) +
                    (displayScore * 0.10) +
                    (ramScore * 0.08) +
                    (gpuInsights.performanceScore * 0.08)
            }
            RecommendationUseCase.OFFICE_STUDY -> {
                (budgetScore * 0.25) +
                    (portabilityScore * 0.20) +
                    (profile.batteryScore * 0.15) +
                    (displayScore * 0.10) +
                    (profile.officeScore * 0.30)
            }
            RecommendationUseCase.PORTABLE_OFFICE -> {
                (portabilityScore * 0.35) +
                    (profile.batteryScore * 0.25) +
                    (profile.officeScore * 0.20) +
                    (budgetScore * 0.10) +
                    (displayScore * 0.10)
            }
            RecommendationUseCase.BATTERY_FIRST -> {
                (profile.batteryScore * 0.45) +
                    (portabilityScore * 0.20) +
                    (profile.officeScore * 0.15) +
                    (budgetScore * 0.10) +
                    (cpuInsights.lowPowerScore * 0.10)
            }
            RecommendationUseCase.CASUAL_GAME -> {
                (gpuInsights.performanceScore * 0.35) +
                    (cpuInsights.performanceScore * 0.20) +
                    (ramScore * 0.15) +
                    (displayScore * 0.10) +
                    (portabilityScore * 0.10) +
                    (budgetScore * 0.10)
            }
            RecommendationUseCase.ONLINE_GAME -> {
                (gpuInsights.performanceScore * 0.40) +
                    (cpuInsights.performanceScore * 0.20) +
                    (ramScore * 0.15) +
                    (tgpScore * 0.10) +
                    (displayScore * 0.10) +
                    (budgetScore * 0.05)
            }
            RecommendationUseCase.AAA_GAME -> {
                (gpuInsights.performanceScore * 0.45) +
                    (tgpScore * 0.20) +
                    (cpuInsights.performanceScore * 0.15) +
                    (ramScore * 0.10) +
                    (displayScore * 0.05) +
                    (budgetScore * 0.05)
            }
            RecommendationUseCase.CREATOR -> {
                (cpuInsights.performanceScore * 0.20) +
                    ((gpuInsights.performanceScore + gpuInsights.creatorBonus).coerceAtMost(100) * 0.20) +
                    (ramScore * 0.20) +
                    (displayScore * 0.20) +
                    (profile.batteryScore * 0.05) +
                    (portabilityScore * 0.05) +
                    (budgetScore * 0.10)
            }
        }

        return ScoreResult(
            score = round(rawScore * 10.0) / 10.0,
            reasons = buildReasons(
                useCase = useCase,
                budgetScore = budgetScore,
                portabilityScore = portabilityScore,
                displayScore = displayScore,
                ramScore = ramScore,
                tgpScore = tgpScore,
                cpuPerformanceScore = cpuInsights.performanceScore,
                lowPowerCpuScore = cpuInsights.lowPowerScore,
                gpuScore = gpuInsights.performanceScore,
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
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> ((profile.officeScore + profile.batteryScore + profile.casualGameScore) / 3.0).roundToInt()
            RecommendationUseCase.OFFICE_STUDY -> profile.officeScore
            RecommendationUseCase.PORTABLE_OFFICE -> profile.officeScore
            RecommendationUseCase.BATTERY_FIRST -> profile.batteryScore
            RecommendationUseCase.CASUAL_GAME -> profile.casualGameScore
            RecommendationUseCase.ONLINE_GAME -> profile.onlineGameScore
            RecommendationUseCase.AAA_GAME -> profile.aaaGameScore
            RecommendationUseCase.CREATOR -> profile.creatorScore
        }
    }

    fun gateThreshold(useCase: RecommendationUseCase): Int {
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> 45
            RecommendationUseCase.OFFICE_STUDY -> 50
            RecommendationUseCase.PORTABLE_OFFICE -> 50
            RecommendationUseCase.BATTERY_FIRST -> 60
            RecommendationUseCase.CASUAL_GAME -> 45
            RecommendationUseCase.ONLINE_GAME -> 55
            RecommendationUseCase.AAA_GAME -> 65
            RecommendationUseCase.CREATOR -> 50
        }
    }

    private fun budgetScore(price: Int?, budget: Int): Int {
        if (price == null || budget <= 0) {
            return 0
        }

        val normalized = (1.0 - (price.toDouble() / budget.toDouble())) * 100.0
        return normalized.coerceIn(0.0, 100.0).roundToInt()
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
