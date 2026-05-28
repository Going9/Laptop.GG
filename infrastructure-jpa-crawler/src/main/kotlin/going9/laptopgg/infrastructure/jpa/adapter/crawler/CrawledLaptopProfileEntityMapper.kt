package going9.laptopgg.infrastructure.jpa.adapter.crawler

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

    fun applySnapshot(profile: LaptopProfile, snapshot: LaptopProfileSnapshot): Boolean {
        var changed = false
        changed = updateField(profile.cpuClass, snapshot.cpuClass) { profile.cpuClass = it } || changed
        changed = updateField(profile.gpuClass, snapshot.gpuClass) { profile.gpuClass = it } || changed
        changed = updateField(profile.batteryTier, snapshot.batteryTier) { profile.batteryTier = it } || changed
        changed = updateField(profile.portabilityTier, snapshot.portabilityTier) { profile.portabilityTier = it } || changed
        changed = updateField(profile.officeScore, snapshot.officeScore) { profile.officeScore = it } || changed
        changed = updateField(profile.batteryScore, snapshot.batteryScore) { profile.batteryScore = it } || changed
        changed = updateField(profile.casualGameScore, snapshot.casualGameScore) { profile.casualGameScore = it } || changed
        changed = updateField(profile.onlineGameScore, snapshot.onlineGameScore) { profile.onlineGameScore = it } || changed
        changed = updateField(profile.aaaGameScore, snapshot.aaaGameScore) { profile.aaaGameScore = it } || changed
        changed = updateField(profile.creatorScore, snapshot.creatorScore) { profile.creatorScore = it } || changed
        changed = updateField(profile.cpuPerformanceScore, snapshot.cpuPerformanceScore) { profile.cpuPerformanceScore = it } || changed
        changed = updateField(profile.lowPowerCpuScore, snapshot.lowPowerCpuScore) { profile.lowPowerCpuScore = it } || changed
        changed = updateField(profile.gpuPerformanceScore, snapshot.gpuPerformanceScore) { profile.gpuPerformanceScore = it } || changed
        changed = updateField(profile.gpuCreatorBonus, snapshot.gpuCreatorBonus) { profile.gpuCreatorBonus = it } || changed
        changed = updateField(profile.portabilityScore, snapshot.portabilityScore) { profile.portabilityScore = it } || changed
        changed = updateField(profile.displayScore, snapshot.displayScore) { profile.displayScore = it } || changed
        changed = updateField(profile.ramScore, snapshot.ramScore) { profile.ramScore = it } || changed
        changed = updateField(profile.tgpScore, snapshot.tgpScore) { profile.tgpScore = it } || changed
        return changed
    }

    fun toState(profile: LaptopProfile): CrawledLaptopProfileState {
        return CrawledLaptopProfileState(
            laptopId = requireNotNull(profile.laptop.id) { "Laptop profile must reference a persisted laptop." },
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

    private fun <T> updateField(currentValue: T, newValue: T, updater: (T) -> Unit): Boolean {
        if (currentValue == newValue) {
            return false
        }

        updater(newValue)
        return true
    }
}
