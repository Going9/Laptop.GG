package going9.laptopgg.application.crawler

import going9.laptopgg.domain.laptop.BatteryTier
import going9.laptopgg.domain.laptop.CpuClass
import going9.laptopgg.domain.laptop.GpuClass
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.PortabilityTier

class LaptopProfileFactory(
    private val cpuClassifier: CpuClassifier = CpuClassifier(),
    private val gpuClassifier: GpuClassifier = GpuClassifier(),
    private val profileScorePolicy: ProfileScorePolicy = ProfileScorePolicy(),
) {
    data class Snapshot(
        val cpuClass: CpuClass,
        val gpuClass: GpuClass,
        val batteryTier: BatteryTier,
        val portabilityTier: PortabilityTier,
        val officeScore: Int,
        val batteryScore: Int,
        val casualGameScore: Int,
        val onlineGameScore: Int,
        val aaaGameScore: Int,
        val creatorScore: Int,
        val cpuPerformanceScore: Int,
        val lowPowerCpuScore: Int,
        val gpuPerformanceScore: Int,
        val gpuCreatorBonus: Int,
        val portabilityScore: Int,
        val displayScore: Int,
        val ramScore: Int,
        val tgpScore: Int,
    )

    fun build(laptop: Laptop): Snapshot {
        val cpu = cpuClassifier.classify(laptop)
        val gpu = gpuClassifier.classify(laptop)
        val scores = profileScorePolicy.calculate(laptop, cpu, gpu)

        return Snapshot(
            cpuClass = cpu.cpuClass,
            gpuClass = gpu.gpuClass,
            batteryTier = scores.batteryTier,
            portabilityTier = scores.portabilityTier,
            officeScore = scores.officeScore,
            batteryScore = scores.batteryScore,
            casualGameScore = scores.casualGameScore,
            onlineGameScore = scores.onlineGameScore,
            aaaGameScore = scores.aaaGameScore,
            creatorScore = scores.creatorScore,
            cpuPerformanceScore = cpu.performanceScore,
            lowPowerCpuScore = cpu.lowPowerScore,
            gpuPerformanceScore = gpu.performanceScore,
            gpuCreatorBonus = gpu.creatorBonus,
            portabilityScore = scores.portabilityScore,
            displayScore = scores.displayScore,
            ramScore = scores.ramScore,
            tgpScore = scores.tgpScore,
        )
    }

    fun resolveCpuInsights(laptop: Laptop): CpuInsights {
        return cpuClassifier.classify(laptop)
    }

    fun resolveGpuInsights(laptop: Laptop): GpuInsights {
        return gpuClassifier.classify(laptop)
    }
}
