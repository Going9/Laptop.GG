package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.assembly.CrawlerPersistenceAssembler
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class CrawlerPersistenceUseCaseConfig {
    @Bean
    fun saveCrawledLaptopService(
        laptopPort: CrawledLaptopPersistencePort,
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopPriceHistoryPort: LaptopPriceHistoryPort,
        recommendationScorePort: RecommendationScorePort,
        transactionPort: CrawlerTransactionPort,
    ): SaveCrawledLaptopUseCase {
        return CrawlerPersistenceAssembler.createSaveCrawledLaptopUseCase(
            laptopPort = laptopPort,
            laptopProfilePort = laptopProfilePort,
            laptopPriceHistoryPort = laptopPriceHistoryPort,
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
    }
}
