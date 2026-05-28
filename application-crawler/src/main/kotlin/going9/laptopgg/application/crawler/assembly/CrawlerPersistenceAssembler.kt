package going9.laptopgg.application.crawler.assembly

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.CrawledLaptopPostSaveSynchronizer
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopLookupLoader
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopService
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.persistence.port.ExistingCrawledLaptopLookupPort
import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort

object CrawlerPersistenceAssembler {
    fun createSaveCrawledLaptopUseCase(
        laptopPort: CrawledLaptopPersistencePort,
        existingLaptopLookupPort: ExistingCrawledLaptopLookupPort,
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopPriceHistoryPort: LaptopPriceHistoryPort,
        recommendationScorePort: RecommendationScorePort,
        transactionPort: CrawlerTransactionPort,
    ): SaveCrawledLaptopUseCase {
        val recommendationScoreService = createRecommendationScoreService(
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
        val laptopProfileService = createLaptopProfileService(
            laptopProfilePort = laptopProfilePort,
            laptopProfileFactory = CrawlerProfileAssembler.createLaptopProfileFactory(),
            recommendationScoreService = recommendationScoreService,
        )
        val laptopPriceHistoryService = createLaptopPriceHistoryService(
            laptopPriceHistoryPort = laptopPriceHistoryPort,
            transactionPort = transactionPort,
        )

        return SaveCrawledLaptopService(
            laptopPort = laptopPort,
            existingLookupLoader = ExistingCrawledLaptopLookupLoader(existingLaptopLookupPort),
            postSaveSynchronizer = CrawledLaptopPostSaveSynchronizer(
                laptopProfileSynchronizer = laptopProfileService,
                laptopPriceHistoryRecorder = laptopPriceHistoryService,
            ),
            transactionPort = transactionPort,
        )
    }

    private fun createRecommendationScoreService(
        recommendationScorePort: RecommendationScorePort,
        transactionPort: CrawlerTransactionPort,
    ): RecommendationScoreService {
        return RecommendationScoreService(
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
    }

    private fun createLaptopProfileService(
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopProfileFactory: LaptopProfileFactory,
        recommendationScoreService: RecommendationScoreService,
    ): LaptopProfileService {
        return LaptopProfileService(
            laptopProfilePort = laptopProfilePort,
            laptopProfileFactory = laptopProfileFactory,
            recommendationScoreRefresher = recommendationScoreService,
        )
    }

    private fun createLaptopPriceHistoryService(
        laptopPriceHistoryPort: LaptopPriceHistoryPort,
        transactionPort: CrawlerTransactionPort,
    ): LaptopPriceHistoryService {
        return LaptopPriceHistoryService(
            laptopPriceHistoryPort = laptopPriceHistoryPort,
            transactionPort = transactionPort,
        )
    }
}
