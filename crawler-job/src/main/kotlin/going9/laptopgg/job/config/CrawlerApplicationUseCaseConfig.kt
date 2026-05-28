package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.CpuClassifier
import going9.laptopgg.application.crawler.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.CrawlerRunLockService
import going9.laptopgg.application.crawler.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.GpuClassifier
import going9.laptopgg.application.crawler.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.LaptopProfileFactory
import going9.laptopgg.application.crawler.LaptopProfileService
import going9.laptopgg.application.crawler.ProfileScorePolicy
import going9.laptopgg.application.crawler.RecommendationScoreService
import going9.laptopgg.application.crawler.SaveCrawledLaptopService
import going9.laptopgg.application.crawler.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.TrackCrawlerRunService
import going9.laptopgg.application.crawler.TrackCrawlerRunUseCase
import going9.laptopgg.application.crawler.port.out.CrawledLaptopPort
import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfilePort
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
        laptopPort: CrawledLaptopPort,
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
        laptopPort: CrawledLaptopPort,
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
