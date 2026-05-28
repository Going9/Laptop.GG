package going9.laptopgg.integration.config

import going9.laptopgg.application.crawler.assembly.CrawlerPersistenceAssembler
import going9.laptopgg.application.crawler.assembly.CrawlerRunAssembler
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.persistence.port.ExistingCrawledLaptopLookupPort
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IntegrationCrawlerUseCaseConfig {
    @Bean
    fun saveCrawledLaptopService(
        laptopPort: CrawledLaptopPersistencePort,
        existingLaptopLookupPort: ExistingCrawledLaptopLookupPort,
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopPriceHistoryPort: LaptopPriceHistoryPort,
        recommendationScorePort: RecommendationScorePort,
        transactionPort: CrawlerTransactionPort,
    ): SaveCrawledLaptopUseCase {
        return CrawlerPersistenceAssembler.createSaveCrawledLaptopUseCase(
            laptopPort = laptopPort,
            existingLaptopLookupPort = existingLaptopLookupPort,
            laptopProfilePort = laptopProfilePort,
            laptopPriceHistoryPort = laptopPriceHistoryPort,
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun crawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunAssembler.createCrawlerRunLockUseCase(crawlerRunLockPort)
    }
}
