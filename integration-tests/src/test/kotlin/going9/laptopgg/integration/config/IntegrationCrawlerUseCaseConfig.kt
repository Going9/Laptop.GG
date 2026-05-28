package going9.laptopgg.integration.config

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopService
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.profile.CpuClassifier
import going9.laptopgg.application.crawler.profile.CpuTokenResolver
import going9.laptopgg.application.crawler.profile.GpuClassifier
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.profile.ProfileScorePolicy
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfileSourcePort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.application.crawler.run.CrawlerRunLockService
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IntegrationCrawlerUseCaseConfig {
    @Bean
    fun cpuTokenResolver(): CpuTokenResolver {
        return CpuTokenResolver()
    }

    @Bean
    fun cpuClassifier(cpuTokenResolver: CpuTokenResolver): CpuClassifier {
        return CpuClassifier(cpuTokenResolver)
    }

    @Bean
    fun gpuClassifier(): GpuClassifier {
        return GpuClassifier()
    }

    @Bean
    fun profileScorePolicy(): ProfileScorePolicy {
        return ProfileScorePolicy()
    }

    @Bean
    fun laptopProfileFactory(
        cpuClassifier: CpuClassifier,
        gpuClassifier: GpuClassifier,
        profileScorePolicy: ProfileScorePolicy,
    ): LaptopProfileFactory {
        return LaptopProfileFactory(
            cpuClassifier = cpuClassifier,
            gpuClassifier = gpuClassifier,
            profileScorePolicy = profileScorePolicy,
        )
    }

    @Bean
    fun recommendationScoreService(
        recommendationScorePort: RecommendationScorePort,
        transactionPort: CrawlerTransactionPort,
    ): RecommendationScoreService {
        return RecommendationScoreService(
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun laptopProfileService(
        laptopPort: CrawledLaptopProfileSourcePort,
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopProfileFactory: LaptopProfileFactory,
        recommendationScoreService: RecommendationScoreService,
        transactionPort: CrawlerTransactionPort,
    ): LaptopProfileService {
        return LaptopProfileService(
            laptopPort = laptopPort,
            laptopProfilePort = laptopProfilePort,
            laptopProfileFactory = laptopProfileFactory,
            recommendationScoreService = recommendationScoreService,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun laptopPriceHistoryService(
        laptopPriceHistoryPort: LaptopPriceHistoryPort,
        transactionPort: CrawlerTransactionPort,
    ): LaptopPriceHistoryService {
        return LaptopPriceHistoryService(
            laptopPriceHistoryPort = laptopPriceHistoryPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun saveCrawledLaptopService(
        laptopPort: CrawledLaptopPersistencePort,
        laptopProfileService: LaptopProfileService,
        laptopPriceHistoryService: LaptopPriceHistoryService,
        transactionPort: CrawlerTransactionPort,
    ): SaveCrawledLaptopUseCase {
        return SaveCrawledLaptopService(
            laptopPort = laptopPort,
            laptopProfileService = laptopProfileService,
            laptopPriceHistoryService = laptopPriceHistoryService,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun crawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunLockService(crawlerRunLockPort)
    }
}
