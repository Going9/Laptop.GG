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

        return if (existingProfile == null) {
            laptopProfileRepository.save(
                LaptopProfile(
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
                ),
            )
        } else {
            if (existingProfile.matches(snapshot)) {
                return existingProfile
            }

            existingProfile.cpuClass = snapshot.cpuClass
            existingProfile.gpuClass = snapshot.gpuClass
            existingProfile.batteryTier = snapshot.batteryTier
            existingProfile.portabilityTier = snapshot.portabilityTier
            existingProfile.officeScore = snapshot.officeScore
            existingProfile.batteryScore = snapshot.batteryScore
            existingProfile.casualGameScore = snapshot.casualGameScore
            existingProfile.onlineGameScore = snapshot.onlineGameScore
            existingProfile.aaaGameScore = snapshot.aaaGameScore
            existingProfile.creatorScore = snapshot.creatorScore
            existingProfile.cpuPerformanceScore = snapshot.cpuPerformanceScore
            existingProfile.lowPowerCpuScore = snapshot.lowPowerCpuScore
            existingProfile.gpuPerformanceScore = snapshot.gpuPerformanceScore
            existingProfile.gpuCreatorBonus = snapshot.gpuCreatorBonus
            existingProfile.portabilityScore = snapshot.portabilityScore
            existingProfile.displayScore = snapshot.displayScore
            existingProfile.ramScore = snapshot.ramScore
            existingProfile.tgpScore = snapshot.tgpScore
            laptopProfileRepository.save(existingProfile)
        }
    }

    private fun LaptopProfile.matches(snapshot: LaptopProfileFactory.Snapshot): Boolean {
        return cpuClass == snapshot.cpuClass &&
            gpuClass == snapshot.gpuClass &&
            batteryTier == snapshot.batteryTier &&
            portabilityTier == snapshot.portabilityTier &&
            officeScore == snapshot.officeScore &&
            batteryScore == snapshot.batteryScore &&
            casualGameScore == snapshot.casualGameScore &&
            onlineGameScore == snapshot.onlineGameScore &&
            aaaGameScore == snapshot.aaaGameScore &&
            creatorScore == snapshot.creatorScore &&
            cpuPerformanceScore == snapshot.cpuPerformanceScore &&
            lowPowerCpuScore == snapshot.lowPowerCpuScore &&
            gpuPerformanceScore == snapshot.gpuPerformanceScore &&
            gpuCreatorBonus == snapshot.gpuCreatorBonus &&
            portabilityScore == snapshot.portabilityScore &&
            displayScore == snapshot.displayScore &&
            ramScore == snapshot.ramScore &&
            tgpScore == snapshot.tgpScore
    }
}
