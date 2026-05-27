package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.domain.repository.LaptopProfileRepository
import going9.laptopgg.domain.repository.LaptopRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopProfileService(
    private val laptopRepository: LaptopRepository,
    private val laptopProfileRepository: LaptopProfileRepository,
    private val laptopProfileFactory: LaptopProfileFactory,
) {
    companion object {
        private const val PROFILE_BACKFILL_BATCH_SIZE = 100
    }

    @Volatile
    private var missingProfilesBackfilled = false
    @Volatile
    private var incompleteProfilesBackfilled = false

    @Transactional
    fun syncMissingProfiles() {
        syncMissingProfilesBatch()
    }

    @Transactional
    fun syncMissingProfilesIfNeeded() {
        if (missingProfilesBackfilled) {
            return
        }

        synchronized(this) {
            if (missingProfilesBackfilled) {
                return
            }

            if (laptopRepository.countWithoutProfile() > 0) {
                syncMissingProfilesBatch()
            }

            missingProfilesBackfilled = laptopRepository.countWithoutProfile() == 0L
        }
    }

    @Transactional
    fun syncIncompleteProfiles() {
        syncIncompleteProfilesBatch()
    }

    @Transactional
    fun syncIncompleteProfilesIfNeeded() {
        if (incompleteProfilesBackfilled) {
            return
        }

        synchronized(this) {
            if (incompleteProfilesBackfilled) {
                return
            }

            if (laptopProfileRepository.countIncompleteStaticScores() > 0) {
                syncIncompleteProfilesBatch()
            }

            incompleteProfilesBackfilled = laptopProfileRepository.countIncompleteStaticScores() == 0L
        }
    }

    @Transactional
    fun syncMissingProfilesBatch(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        val ids = laptopRepository.findIdsWithoutProfile(PageRequest.of(0, limit))
        if (ids.isEmpty()) {
            missingProfilesBackfilled = true
            return 0
        }

        laptopRepository.findAllWithUsageByIdIn(ids)
            .forEach { laptop -> syncProfile(laptop) }

        return ids.size
    }

    @Transactional
    fun syncIncompleteProfilesBatch(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        val ids = laptopProfileRepository.findLaptopIdsWithIncompleteStaticScores(PageRequest.of(0, limit))
        if (ids.isEmpty()) {
            incompleteProfilesBackfilled = true
            return 0
        }

        laptopRepository.findAllWithUsageByIdIn(ids)
            .forEach { laptop -> syncProfile(laptop) }

        return ids.size
    }

    @Transactional
    fun syncProfile(laptop: Laptop): LaptopProfile {
        val laptopId = requireNotNull(laptop.id) { "Laptop must be persisted before syncing a profile." }
        val snapshot = laptopProfileFactory.build(laptop)
        val existingProfile = laptopProfileRepository.findByLaptopId(laptopId)

        if (existingProfile == null) {
            return laptopProfileRepository.save(newProfile(laptop, snapshot))
        }

        if (!existingProfile.applySnapshot(snapshot)) {
            return existingProfile
        }

        return laptopProfileRepository.save(existingProfile)
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
