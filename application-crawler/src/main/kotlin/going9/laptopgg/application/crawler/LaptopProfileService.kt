package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawledLaptopPort
import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfilePort
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import org.springframework.transaction.annotation.Transactional

@Transactional
class LaptopProfileService(
    private val laptopPort: CrawledLaptopPort,
    private val laptopProfilePort: CrawledLaptopProfilePort,
    private val laptopProfileFactory: LaptopProfileFactory,
    private val recommendationScoreService: RecommendationScoreService,
) {
    companion object {
        private const val PROFILE_BACKFILL_BATCH_SIZE = 100
    }

    @Volatile
    private var missingProfilesBackfilled = false
    @Volatile
    private var incompleteProfilesBackfilled = false

    fun syncMissingProfiles() {
        syncMissingProfilesBatch()
    }

    fun syncMissingProfilesIfNeeded() {
        if (missingProfilesBackfilled) {
            return
        }

        synchronized(this) {
            if (missingProfilesBackfilled) {
                return
            }

            if (hasMissingProfiles()) {
                syncMissingProfilesBatch()
            }

            missingProfilesBackfilled = !hasMissingProfiles()
        }
    }

    fun syncIncompleteProfiles() {
        syncIncompleteProfilesBatch()
    }

    fun syncIncompleteProfilesIfNeeded() {
        if (incompleteProfilesBackfilled) {
            return
        }

        synchronized(this) {
            if (incompleteProfilesBackfilled) {
                return
            }

            if (hasIncompleteProfiles()) {
                syncIncompleteProfilesBatch()
            }

            incompleteProfilesBackfilled = !hasIncompleteProfiles()
        }
    }

    fun syncMissingProfilesBatch(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        val ids = laptopPort.findIdsWithoutProfile(limit)
        if (ids.isEmpty()) {
            missingProfilesBackfilled = true
            return 0
        }

        laptopPort.findAllWithUsageByIds(ids)
            .forEach { laptop -> syncProfile(laptop) }

        return ids.size
    }

    fun syncIncompleteProfilesBatch(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        val ids = laptopProfilePort.findLaptopIdsWithIncompleteStaticScores(limit)
        if (ids.isEmpty()) {
            incompleteProfilesBackfilled = true
            return 0
        }

        laptopPort.findAllWithUsageByIds(ids)
            .forEach { laptop -> syncProfile(laptop) }

        return ids.size
    }

    fun syncProfile(laptop: Laptop): LaptopProfile {
        val laptopId = requireNotNull(laptop.id) { "Laptop must be persisted before syncing a profile." }
        val snapshot = laptopProfileFactory.build(laptop)
        val existingProfile = laptopProfilePort.findByLaptopId(laptopId)

        val profile = if (existingProfile == null) {
            laptopProfilePort.save(newProfile(laptop, snapshot))
        } else if (existingProfile.applySnapshot(snapshot)) {
            laptopProfilePort.save(existingProfile)
        } else {
            existingProfile
        }

        recommendationScoreService.refreshScores(profile)
        return profile
    }

    private fun newProfile(laptop: Laptop, snapshot: LaptopProfileFactory.Snapshot): LaptopProfile {
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

    private fun hasMissingProfiles(): Boolean {
        return laptopPort.findIdsWithoutProfile(1).isNotEmpty()
    }

    private fun hasIncompleteProfiles(): Boolean {
        return laptopProfilePort.findLaptopIdsWithIncompleteStaticScores(1).isNotEmpty()
    }

    private fun LaptopProfile.applySnapshot(snapshot: LaptopProfileFactory.Snapshot): Boolean {
        var changed = false
        changed = updateField(cpuClass, snapshot.cpuClass) { cpuClass = it } || changed
        changed = updateField(gpuClass, snapshot.gpuClass) { gpuClass = it } || changed
        changed = updateField(batteryTier, snapshot.batteryTier) { batteryTier = it } || changed
        changed = updateField(portabilityTier, snapshot.portabilityTier) { portabilityTier = it } || changed
        changed = updateField(officeScore, snapshot.officeScore) { officeScore = it } || changed
        changed = updateField(batteryScore, snapshot.batteryScore) { batteryScore = it } || changed
        changed = updateField(casualGameScore, snapshot.casualGameScore) { casualGameScore = it } || changed
        changed = updateField(onlineGameScore, snapshot.onlineGameScore) { onlineGameScore = it } || changed
        changed = updateField(aaaGameScore, snapshot.aaaGameScore) { aaaGameScore = it } || changed
        changed = updateField(creatorScore, snapshot.creatorScore) { creatorScore = it } || changed
        changed = updateField(cpuPerformanceScore, snapshot.cpuPerformanceScore) { cpuPerformanceScore = it } || changed
        changed = updateField(lowPowerCpuScore, snapshot.lowPowerCpuScore) { lowPowerCpuScore = it } || changed
        changed = updateField(gpuPerformanceScore, snapshot.gpuPerformanceScore) { gpuPerformanceScore = it } || changed
        changed = updateField(gpuCreatorBonus, snapshot.gpuCreatorBonus) { gpuCreatorBonus = it } || changed
        changed = updateField(portabilityScore, snapshot.portabilityScore) { portabilityScore = it } || changed
        changed = updateField(displayScore, snapshot.displayScore) { displayScore = it } || changed
        changed = updateField(ramScore, snapshot.ramScore) { ramScore = it } || changed
        changed = updateField(tgpScore, snapshot.tgpScore) { tgpScore = it } || changed
        return changed
    }

    private fun <T> updateField(currentValue: T, newValue: T, updater: (T) -> Unit): Boolean {
        if (currentValue == newValue) {
            return false
        }

        updater(newValue)
        return true
    }
}
