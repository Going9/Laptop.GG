package going9.laptopgg.application.crawler.profile

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreRefresher

interface SyncCrawledLaptopProfileUseCase {
    fun syncProfile(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState
}

internal interface CrawledLaptopProfileSynchronizer {
    fun syncProfileInTransaction(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState
}

internal class LaptopProfileService(
    private val laptopProfilePort: CrawledLaptopProfilePort,
    private val laptopProfileFactory: LaptopProfileFactory,
    private val recommendationScoreRefresher: RecommendationScoreRefresher,
    private val transactionPort: CrawlerTransactionPort,
) : SyncCrawledLaptopProfileUseCase, CrawledLaptopProfileSynchronizer {
    override fun syncProfile(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState {
        return transactionPort.write {
            syncProfileInTransaction(laptop)
        }
    }

    override fun syncProfileInTransaction(laptop: PersistedCrawledLaptopSnapshot): CrawledLaptopProfileState {
        val laptopId = laptop.id
        val snapshot = laptopProfileFactory.build(laptop)
        val profile = laptopProfilePort.upsert(
            UpsertCrawledLaptopProfileCommand(
                laptopId = laptopId,
                profile = snapshot,
            ),
        )

        recommendationScoreRefresher.refreshScoresInTransaction(profile)
        return profile
    }
}
