package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.profile.CpuClassifier
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.run.CrawlerRunLockService
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.profile.GpuClassifier
import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.profile.ProfileScorePolicy
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopService
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.run.TrackCrawlerRunService
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.application.crawler.port.out.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfileSourcePort
import going9.laptopgg.application.crawler.port.out.CrawlerRunLockPort
import going9.laptopgg.application.crawler.port.out.CrawlerRunPort
import going9.laptopgg.application.crawler.port.out.CrawlerTransactionPort
import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.port.out.RecommendationScorePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class CrawlerApplicationUseCaseConfig {
    @Bean
    fun cpuClassifier(): CpuClassifier {
        return CpuClassifier()
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
    fun crawledCpuModelResolver(cpuClassifier: CpuClassifier): CrawledCpuModelResolver {
        return CrawledCpuModelResolver(cpuClassifier)
    }

    @Bean
    fun crawledGraphicsModelResolver(gpuClassifier: GpuClassifier): CrawledGraphicsModelResolver {
        return CrawledGraphicsModelResolver(gpuClassifier)
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
    fun trackCrawlerRunService(
        crawlerRunPort: CrawlerRunPort,
        transactionPort: CrawlerTransactionPort,
    ): TrackCrawlerRunUseCase {
        return TrackCrawlerRunService(
            crawlerRunPort = crawlerRunPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun crawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunLockService(crawlerRunLockPort)
    }
}
