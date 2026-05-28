package going9.laptopgg.application.crawler

import going9.laptopgg.domain.laptop.Laptop

class LaptopProfileFactory(
    private val cpuClassifier: CpuClassifier = CpuClassifier(),
    private val gpuClassifier: GpuClassifier = GpuClassifier(),
    private val profileScorePolicy: ProfileScorePolicy = ProfileScorePolicy(),
) {
    fun build(laptop: Laptop): LaptopProfileSnapshot {
        val cpu = cpuClassifier.classify(laptop)
        val gpu = gpuClassifier.classify(laptop)
        val scores = profileScorePolicy.calculate(laptop, cpu, gpu)

        return LaptopProfileSnapshot(
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
