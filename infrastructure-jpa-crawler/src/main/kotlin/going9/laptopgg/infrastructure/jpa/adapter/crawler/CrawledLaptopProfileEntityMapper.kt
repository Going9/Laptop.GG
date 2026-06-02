package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopProfile

internal object CrawledLaptopProfileEntityMapper {
    fun newProfile(laptop: Laptop, snapshot: LaptopProfileSnapshot): LaptopProfile {
        return LaptopProfile(
            laptop = laptop,
            cpuClass = snapshot.cpuClass,
            gpuClass = snapshot.gpuClass,
            batteryTier = snapshot.batteryTier,
            portabilityTier = snapshot.portabilityTier,
            officeScore = snapshot.officeScore,
            batteryScore = snapshot.batteryScore,
            casualGameScore = snapshot.casualGameScore,
            onlineGameScore = snapshot.onlineGameScore,
            aaaGameScore = snapshot.aaaGameScore,
            creatorScore = snapshot.creatorScore,
            cpuPerformanceScore = snapshot.cpuPerformanceScore,
            lowPowerCpuScore = snapshot.lowPowerCpuScore,
            gpuPerformanceScore = snapshot.gpuPerformanceScore,
            gpuCreatorBonus = snapshot.gpuCreatorBonus,
            portabilityScore = snapshot.portabilityScore,
            displayScore = snapshot.displayScore,
            ramScore = snapshot.ramScore,
            tgpScore = snapshot.tgpScore,
        )
    }

    fun toState(profile: LaptopProfile): CrawledLaptopProfileState {
        return CrawledLaptopProfileState(
            laptopId = profile.laptop.id
                ?: throw CrawlerInvalidStateException("Laptop profile must reference a persisted laptop."),
            profile = LaptopProfileSnapshot(
                cpuClass = profile.cpuClass,
                gpuClass = profile.gpuClass,
                batteryTier = profile.batteryTier,
                portabilityTier = profile.portabilityTier,
                officeScore = profile.officeScore,
                batteryScore = profile.batteryScore,
                casualGameScore = profile.casualGameScore,
                onlineGameScore = profile.onlineGameScore,
                aaaGameScore = profile.aaaGameScore,
                creatorScore = profile.creatorScore,
                cpuPerformanceScore = profile.cpuPerformanceScore,
                lowPowerCpuScore = profile.lowPowerCpuScore,
                gpuPerformanceScore = profile.gpuPerformanceScore,
                gpuCreatorBonus = profile.gpuCreatorBonus,
                portabilityScore = profile.portabilityScore,
                displayScore = profile.displayScore,
                ramScore = profile.ramScore,
                tgpScore = profile.tgpScore,
            ),
        )
    }
}
