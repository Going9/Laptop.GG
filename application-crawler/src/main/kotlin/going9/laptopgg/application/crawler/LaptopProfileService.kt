package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawledLaptopPort
import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfilePort
import going9.laptopgg.domain.laptop.Laptop
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

    fun syncProfile(laptop: Laptop): CrawledLaptopProfileState {
        val laptopId = requireNotNull(laptop.id) { "Laptop must be persisted before syncing a profile." }
        val snapshot = laptopProfileFactory.build(laptop)
        val profile = laptopProfilePort.upsert(
            UpsertCrawledLaptopProfileCommand(
                laptopId = laptopId,
                profile = snapshot,
            ),
        )

        recommendationScoreService.refreshScores(profile)
        return profile
    }

    private fun hasMissingProfiles(): Boolean {
        return laptopPort.findIdsWithoutProfile(1).isNotEmpty()
    }

    private fun hasIncompleteProfiles(): Boolean {
        return laptopProfilePort.findLaptopIdsWithIncompleteStaticScores(1).isNotEmpty()
    }
}
