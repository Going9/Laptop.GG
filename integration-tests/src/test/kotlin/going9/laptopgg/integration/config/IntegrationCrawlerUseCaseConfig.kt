package going9.laptopgg.integration.config

import going9.laptopgg.application.crawler.assembly.CrawlerPersistenceAssembler
import going9.laptopgg.application.crawler.assembly.CrawlerProfileAssembler
import going9.laptopgg.application.crawler.assembly.CrawlerRunAssembler
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
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
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IntegrationCrawlerUseCaseConfig {
    @Bean
    fun cpuTokenResolver(): CpuTokenResolver {
        return CrawlerProfileAssembler.createCpuTokenResolver()
    }

    @Bean
    fun cpuClassifier(cpuTokenResolver: CpuTokenResolver): CpuClassifier {
        return CrawlerProfileAssembler.createCpuClassifier(cpuTokenResolver)
    }

    @Bean
    fun gpuClassifier(): GpuClassifier {
        return CrawlerProfileAssembler.createGpuClassifier()
    }

    @Bean
    fun profileScorePolicy(): ProfileScorePolicy {
        return CrawlerProfileAssembler.createProfileScorePolicy()
    }

    @Bean
    fun laptopProfileFactory(
        cpuClassifier: CpuClassifier,
        gpuClassifier: GpuClassifier,
        profileScorePolicy: ProfileScorePolicy,
    ): LaptopProfileFactory {
        return CrawlerProfileAssembler.createLaptopProfileFactory(
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
        return CrawlerPersistenceAssembler.createRecommendationScoreService(
            recommendationScorePort = recommendationScorePort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun laptopProfileService(
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopProfileFactory: LaptopProfileFactory,
        recommendationScoreService: RecommendationScoreService,
        transactionPort: CrawlerTransactionPort,
    ): LaptopProfileService {
        return CrawlerPersistenceAssembler.createLaptopProfileService(
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
        return CrawlerPersistenceAssembler.createLaptopPriceHistoryService(
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
        return CrawlerPersistenceAssembler.createSaveCrawledLaptopUseCase(
            laptopPort = laptopPort,
            laptopProfileService = laptopProfileService,
            laptopPriceHistoryService = laptopPriceHistoryService,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun crawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunAssembler.createCrawlerRunLockUseCase(crawlerRunLockPort)
    }
}
