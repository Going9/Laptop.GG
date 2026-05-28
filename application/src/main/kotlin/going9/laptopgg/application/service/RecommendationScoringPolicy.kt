package going9.laptopgg.application.service

import going9.laptopgg.application.recommendation.RecommendationUseCase
import going9.laptopgg.domain.laptop.LaptopProfile
import kotlin.math.roundToInt

class RecommendationScoringPolicy {
    fun staticScore(useCase: RecommendationUseCase, inputs: RecommendationScoreInputs): Double {
        return weightedScore(useCase, inputs.copy(budgetScore = 0))
    }

    fun budgetWeight(useCase: RecommendationUseCase): Double {
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> 0.14
            RecommendationUseCase.OFFICE_STUDY -> 0.25
            RecommendationUseCase.PORTABLE_OFFICE -> 0.10
            RecommendationUseCase.BATTERY_FIRST -> 0.10
            RecommendationUseCase.CASUAL_GAME -> 0.10
            RecommendationUseCase.ONLINE_GAME -> 0.05
            RecommendationUseCase.AAA_GAME -> 0.05
            RecommendationUseCase.CREATOR -> 0.10
        }
    }

    fun weightedScore(useCase: RecommendationUseCase, inputs: RecommendationScoreInputs): Double {
        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> {
                (inputs.officeScore * 0.24) +
                    (inputs.batteryScore * 0.20) +
                    (inputs.portabilityScore * 0.16) +
                    (inputs.budgetScore * 0.14) +
                    (inputs.displayScore * 0.10) +
                    (inputs.ramScore * 0.08) +
                    (inputs.gpuScore * 0.08)
            }
            RecommendationUseCase.OFFICE_STUDY -> {
                (inputs.budgetScore * 0.25) +
                    (inputs.portabilityScore * 0.20) +
                    (inputs.batteryScore * 0.15) +
                    (inputs.displayScore * 0.10) +
                    (inputs.officeScore * 0.30)
            }
            RecommendationUseCase.PORTABLE_OFFICE -> {
                (inputs.portabilityScore * 0.35) +
                    (inputs.batteryScore * 0.25) +
                    (inputs.officeScore * 0.20) +
                    (inputs.budgetScore * 0.10) +
                    (inputs.displayScore * 0.10)
            }
            RecommendationUseCase.BATTERY_FIRST -> {
                (inputs.batteryScore * 0.45) +
                    (inputs.portabilityScore * 0.20) +
                    (inputs.officeScore * 0.15) +
                    (inputs.budgetScore * 0.10) +
                    (inputs.lowPowerCpuScore * 0.10)
            }
            RecommendationUseCase.CASUAL_GAME -> {
                (inputs.gpuScore * 0.35) +
                    (inputs.cpuPerformanceScore * 0.20) +
                    (inputs.ramScore * 0.15) +
                    (inputs.displayScore * 0.10) +
                    (inputs.portabilityScore * 0.10) +
                    (inputs.budgetScore * 0.10)
            }
            RecommendationUseCase.ONLINE_GAME -> {
                (inputs.gpuScore * 0.40) +
                    (inputs.cpuPerformanceScore * 0.20) +
                    (inputs.ramScore * 0.15) +
                    (inputs.tgpScore * 0.10) +
                    (inputs.displayScore * 0.10) +
                    (inputs.budgetScore * 0.05)
            }
            RecommendationUseCase.AAA_GAME -> {
                (inputs.gpuScore * 0.45) +
                    (inputs.tgpScore * 0.20) +
                    (inputs.cpuPerformanceScore * 0.15) +
                    (inputs.ramScore * 0.10) +
                    (inputs.displayScore * 0.05) +
                    (inputs.budgetScore * 0.05)
            }
            RecommendationUseCase.CREATOR -> {
                (inputs.cpuPerformanceScore * 0.20) +
                    (inputs.creatorGpuScore * 0.20) +
                    (inputs.ramScore * 0.20) +
                    (inputs.displayScore * 0.20) +
                    (inputs.batteryScore * 0.05) +
                    (inputs.portabilityScore * 0.05) +
                    (inputs.budgetScore * 0.10)
            }
        }
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
}

data class RecommendationScoreInputs(
    val budgetScore: Int,
    val portabilityScore: Int,
    val displayScore: Int,
    val ramScore: Int,
    val tgpScore: Int,
    val cpuPerformanceScore: Int,
    val lowPowerCpuScore: Int,
    val gpuScore: Int,
    val creatorGpuScore: Int,
    val officeScore: Int,
    val batteryScore: Int,
)
