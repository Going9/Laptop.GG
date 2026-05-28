package going9.laptopgg.application.crawler.profile

import kotlin.math.roundToInt

internal data class ProfileUseCaseScores(
    val officeScore: Int,
    val batteryScore: Int,
    val casualGameScore: Int,
    val onlineGameScore: Int,
    val aaaGameScore: Int,
    val creatorScore: Int,
)

internal class ProfileUseCaseScorePolicy {
    fun calculate(cpu: CpuInsights, gpu: GpuInsights, metrics: ProfileMetrics): ProfileUseCaseScores {
        val usageBoosts = metrics.usageBoosts

        return ProfileUseCaseScores(
            officeScore = clampScore(
                cpu.performanceScore * 0.40 +
                    metrics.ramScore * 0.20 +
                    metrics.displayScore * 0.15 +
                    metrics.portabilityScore * 0.15 +
                    cpu.lowPowerScore * 0.10 +
                    usageBoosts.officeBoost,
            ),
            batteryScore = clampScore(
                metrics.batteryCapacityScore * 0.60 +
                    cpu.lowPowerScore * 0.25 +
                    metrics.portabilityScore * 0.15 +
                    usageBoosts.portableBoost,
            ),
            casualGameScore = clampScore(
                gpu.performanceScore * 0.45 +
                    cpu.performanceScore * 0.20 +
                    metrics.ramScore * 0.15 +
                    metrics.displayScore * 0.10 +
                    metrics.tgpScore * 0.10 +
                    usageBoosts.gameBoost,
            ),
            onlineGameScore = clampScore(
                gpu.performanceScore * 0.55 +
                    cpu.performanceScore * 0.20 +
                    metrics.ramScore * 0.10 +
                    metrics.tgpScore * 0.15 +
                    usageBoosts.gameBoost,
            ),
            aaaGameScore = clampScore(
                gpu.performanceScore * 0.65 +
                    cpu.performanceScore * 0.15 +
                    metrics.ramScore * 0.10 +
                    metrics.tgpScore * 0.10 +
                    usageBoosts.gameBoost,
            ),
            creatorScore = clampScore(
                cpu.performanceScore * 0.30 +
                    (gpu.performanceScore + gpu.creatorBonus) * 0.25 +
                    metrics.ramScore * 0.20 +
                    metrics.displayScore * 0.15 +
                    metrics.batteryCapacityScore * 0.10 +
                    usageBoosts.creatorBoost,
            ),
        )
    }

    private fun clampScore(value: Double): Int {
        return value.roundToInt().coerceIn(0, 100)
    }
}
