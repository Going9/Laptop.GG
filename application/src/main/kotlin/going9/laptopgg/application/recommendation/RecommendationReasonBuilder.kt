package going9.laptopgg.application.recommendation

import going9.laptopgg.application.recommendation.port.RecommendationCandidateRecord
import going9.laptopgg.recommendation.RecommendationUseCase
import kotlin.math.roundToInt

internal class RecommendationReasonBuilder {
    fun build(
        useCase: RecommendationUseCase,
        candidate: RecommendationCandidateRecord,
        budgetScore: Int,
    ): List<String> {
        val balancedScore = ((candidate.officeScore + candidate.batteryScore + candidate.casualGameScore) / 3.0)
            .roundToInt()
        val candidates = when (useCase) {
            RecommendationUseCase.NOT_SURE -> listOf(
                "일상용으로 두루 잘 맞아요" to balancedScore,
                "배터리가 오래가는 편이에요" to candidate.batteryScore,
                "들고 다니기 편한 편이에요" to candidate.portabilityScore,
                "예산에 잘 맞아요" to budgetScore,
                "문서 작업에 잘 맞아요" to candidate.officeScore,
            )
            RecommendationUseCase.OFFICE_STUDY -> listOf(
                "문서 작업에 잘 맞아요" to candidate.officeScore,
                "들고 다니기 편한 편이에요" to candidate.portabilityScore,
                "배터리가 오래가는 편이에요" to candidate.batteryScore,
                "화면이 보기 편해요" to candidate.displayScore,
                "예산에 잘 맞아요" to budgetScore,
            )
            RecommendationUseCase.PORTABLE_OFFICE -> listOf(
                "가볍게 들고 다니기 좋아요" to candidate.portabilityScore,
                "배터리가 오래가는 편이에요" to candidate.batteryScore,
                "업무용으로 무난해요" to candidate.officeScore,
                "화면이 보기 편해요" to candidate.displayScore,
                "예산에 잘 맞아요" to budgetScore,
            )
            RecommendationUseCase.BATTERY_FIRST -> listOf(
                "배터리가 오래가는 편이에요" to candidate.batteryScore,
                "전력 효율이 좋은 편이에요" to candidate.lowPowerCpuScore,
                "들고 다니기 편한 편이에요" to candidate.portabilityScore,
                "문서 작업에 잘 맞아요" to candidate.officeScore,
                "예산에 잘 맞아요" to budgetScore,
            )
            RecommendationUseCase.CASUAL_GAME -> listOf(
                "가벼운 게임에 잘 맞아요" to candidate.casualGameScore,
                "그래픽이 괜찮은 편이에요" to candidate.gpuPerformanceScore,
                "메모리가 넉넉한 편이에요" to candidate.ramScore,
                "들고 다니기 무난해요" to candidate.portabilityScore,
                "화면이 보기 편해요" to candidate.displayScore,
            )
            RecommendationUseCase.ONLINE_GAME -> listOf(
                "온라인 게임에 잘 맞아요" to candidate.onlineGameScore,
                "그래픽이 좋은 편이에요" to candidate.gpuPerformanceScore,
                "프로세서가 빠른 편이에요" to candidate.cpuPerformanceScore,
                "게임 성능 여유가 있어요" to candidate.tgpScore,
                "메모리가 넉넉한 편이에요" to candidate.ramScore,
            )
            RecommendationUseCase.AAA_GAME -> listOf(
                "고사양 게임에 잘 맞아요" to candidate.aaaGameScore,
                "그래픽이 좋은 편이에요" to candidate.gpuPerformanceScore,
                "게임 성능 여유가 있어요" to candidate.tgpScore,
                "프로세서가 빠른 편이에요" to candidate.cpuPerformanceScore,
                "메모리가 넉넉한 편이에요" to candidate.ramScore,
            )
            RecommendationUseCase.CREATOR -> listOf(
                "사진·영상 작업에 잘 맞아요" to candidate.creatorScore,
                "프로세서가 빠른 편이에요" to candidate.cpuPerformanceScore,
                "그래픽이 좋은 편이에요" to candidate.gpuPerformanceScore,
                "메모리가 넉넉한 편이에요" to candidate.ramScore,
                "화면이 보기 편해요" to candidate.displayScore,
            )
        }

        return candidates
            .sortedByDescending { it.second }
            .map { it.first }
            .take(REASON_COUNT)
    }

    private companion object {
        const val REASON_COUNT = 2
    }
}
