package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.PortabilityTier

internal data class ProfileScores(
    val batteryTier: BatteryTier,
    val portabilityTier: PortabilityTier,
    val officeScore: Int,
    val batteryScore: Int,
    val casualGameScore: Int,
    val onlineGameScore: Int,
    val aaaGameScore: Int,
    val creatorScore: Int,
    val portabilityScore: Int,
    val displayScore: Int,
    val ramScore: Int,
    val tgpScore: Int,
)

internal class ProfileScorePolicy(
    private val profileMetricPolicy: ProfileMetricPolicy = ProfileMetricPolicy(),
    private val profileUseCaseScorePolicy: ProfileUseCaseScorePolicy = ProfileUseCaseScorePolicy(),
) {
    fun calculate(laptop: LaptopProfileSource, cpu: CpuInsights, gpu: GpuInsights): ProfileScores {
        val metrics = profileMetricPolicy.calculate(laptop, gpu)
        val useCaseScores = profileUseCaseScorePolicy.calculate(cpu, gpu, metrics)

        return ProfileScores(
            batteryTier = metrics.batteryTier,
            portabilityTier = metrics.portabilityTier,
            officeScore = useCaseScores.officeScore,
            batteryScore = useCaseScores.batteryScore,
            casualGameScore = useCaseScores.casualGameScore,
            onlineGameScore = useCaseScores.onlineGameScore,
            aaaGameScore = useCaseScores.aaaGameScore,
            creatorScore = useCaseScores.creatorScore,
            portabilityScore = metrics.portabilityScore,
            displayScore = metrics.displayScore,
            ramScore = metrics.ramScore,
            tgpScore = metrics.tgpScore,
        )
    }
}
