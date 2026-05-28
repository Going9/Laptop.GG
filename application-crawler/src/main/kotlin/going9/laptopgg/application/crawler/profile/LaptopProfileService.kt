package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfileSourcePort
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService

class LaptopProfileService(
    private val laptopPort: CrawledLaptopProfileSourcePort,
    private val laptopProfilePort: CrawledLaptopProfilePort,
    private val laptopProfileFactory: LaptopProfileFactory,
    private val recommendationScoreService: RecommendationScoreService,
    private val transactionPort: CrawlerTransactionPort,
) {
    companion object {
        private const val PROFILE_BACKFILL_BATCH_SIZE = 100
    }

    @Volatile
    private var missingProfilesBackfilled = false
    @Volatile
    private var incompleteProfilesBackfilled = false

    fun syncMissingProfiles() {
        transactionPort.write {
            syncMissingProfilesBatchInTransaction()
        }
    }

    fun syncMissingProfilesIfNeeded() {
        if (missingProfilesBackfilled) {
            return
        }

        transactionPort.write {
            synchronized(this) {
                if (missingProfilesBackfilled) {
                    return@write
                }

                if (hasMissingProfiles()) {
                    syncMissingProfilesBatchInTransaction()
                }

                missingProfilesBackfilled = !hasMissingProfiles()
            }
        }
    }

    fun syncIncompleteProfiles() {
        transactionPort.write {
            syncIncompleteProfilesBatchInTransaction()
        }
    }

    fun syncIncompleteProfilesIfNeeded() {
        if (incompleteProfilesBackfilled) {
            return
        }

        transactionPort.write {
            synchronized(this) {
                if (incompleteProfilesBackfilled) {
                    return@write
                }

                if (hasIncompleteProfiles()) {
                    syncIncompleteProfilesBatchInTransaction()
                }

                incompleteProfilesBackfilled = !hasIncompleteProfiles()
            }
        }
    }

    fun syncMissingProfilesBatch(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        return transactionPort.write {
            syncMissingProfilesBatchInTransaction(limit)
        }
    }

    fun syncIncompleteProfilesBatch(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        return transactionPort.write {
            syncIncompleteProfilesBatchInTransaction(limit)
        }
    }

    fun syncProfile(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState {
        return transactionPort.write {
            syncProfileInTransaction(laptop)
        }
    }

    private fun syncMissingProfilesBatchInTransaction(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        val ids = laptopPort.findIdsWithoutProfile(limit)
        if (ids.isEmpty()) {
            missingProfilesBackfilled = true
            return 0
        }

        laptopPort.findAllWithUsageByIds(ids)
            .forEach { laptop -> syncProfileInTransaction(laptop) }

        return ids.size
    }

    private fun syncIncompleteProfilesBatchInTransaction(limit: Int = PROFILE_BACKFILL_BATCH_SIZE): Int {
        val ids = laptopProfilePort.findLaptopIdsWithIncompleteStaticScores(limit)
        if (ids.isEmpty()) {
            incompleteProfilesBackfilled = true
            return 0
        }

        laptopPort.findAllWithUsageByIds(ids)
            .forEach { laptop -> syncProfileInTransaction(laptop) }

        return ids.size
    }

    internal fun syncProfileInTransaction(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState {
        val laptopId = laptop.id
        val snapshot = laptopProfileFactory.build(laptop)
        val profile = laptopProfilePort.upsert(
            UpsertCrawledLaptopProfileCommand(
                laptopId = laptopId,
                profile = snapshot,
            ),
        )

        recommendationScoreService.refreshScoresInTransaction(profile)
        return profile
    }

    private fun hasMissingProfiles(): Boolean {
        return laptopPort.findIdsWithoutProfile(1).isNotEmpty()
    }

    private fun hasIncompleteProfiles(): Boolean {
        return laptopProfilePort.findLaptopIdsWithIncompleteStaticScores(1).isNotEmpty()
    }
}
