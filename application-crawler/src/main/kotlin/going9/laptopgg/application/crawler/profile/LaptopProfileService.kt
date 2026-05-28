package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService

class LaptopProfileService(
    private val laptopProfilePort: CrawledLaptopProfilePort,
    private val laptopProfileFactory: LaptopProfileFactory,
    private val recommendationScoreService: RecommendationScoreService,
    private val transactionPort: CrawlerTransactionPort,
) {
    fun syncProfile(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState {
        return transactionPort.write {
            syncProfileInTransaction(laptop)
        }
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
}
