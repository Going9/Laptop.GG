package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot

internal class LaptopProfileFactory(
    private val cpuClassifier: CpuClassifier = CpuClassifier(),
    private val gpuClassifier: GpuClassifier = GpuClassifier(),
    private val profileScorePolicy: ProfileScorePolicy = ProfileScorePolicy(),
) {
    fun build(laptop: PersistedCrawledLaptopSnapshot): LaptopProfileSnapshot {
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

    fun resolveCpuInsights(laptop: PersistedCrawledLaptopSnapshot): CpuInsights {
        return cpuClassifier.classify(laptop)
    }

    fun resolveGpuInsights(laptop: PersistedCrawledLaptopSnapshot): GpuInsights {
        return gpuClassifier.classify(laptop)
    }
}
