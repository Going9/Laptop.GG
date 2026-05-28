package going9.laptopgg.application.crawler.assembly

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.CrawledLaptopPostSaveSynchronizer
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopLookupLoader
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopService
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort

object CrawlerPersistenceAssembler {
    fun createRecommendationScoreService(
        recommendationScorePort: RecommendationScorePort,
        transactionPort: CrawlerTransactionPort,
    ): RecommendationScoreService {
        return RecommendationScoreService(
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
    }

    fun createLaptopProfileService(
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopProfileFactory: LaptopProfileFactory,
        recommendationScoreService: RecommendationScoreService,
        transactionPort: CrawlerTransactionPort,
    ): LaptopProfileService {
        return LaptopProfileService(
            laptopProfilePort = laptopProfilePort,
            laptopProfileFactory = laptopProfileFactory,
            recommendationScoreService = recommendationScoreService,
            transactionPort = transactionPort,
        )
    }

    fun createLaptopPriceHistoryService(
        laptopPriceHistoryPort: LaptopPriceHistoryPort,
        transactionPort: CrawlerTransactionPort,
    ): LaptopPriceHistoryService {
        return LaptopPriceHistoryService(
            laptopPriceHistoryPort = laptopPriceHistoryPort,
            transactionPort = transactionPort,
        )
    }

    fun createSaveCrawledLaptopUseCase(
        laptopPort: CrawledLaptopPersistencePort,
        laptopProfileService: LaptopProfileService,
        laptopPriceHistoryService: LaptopPriceHistoryService,
        transactionPort: CrawlerTransactionPort,
    ): SaveCrawledLaptopUseCase {
        return SaveCrawledLaptopService(
            laptopPort = laptopPort,
            existingLookupLoader = ExistingCrawledLaptopLookupLoader(laptopPort),
            postSaveSynchronizer = CrawledLaptopPostSaveSynchronizer(
                laptopProfileService = laptopProfileService,
                laptopPriceHistoryService = laptopPriceHistoryService,
            ),
            transactionPort = transactionPort,
        )
    }
}
