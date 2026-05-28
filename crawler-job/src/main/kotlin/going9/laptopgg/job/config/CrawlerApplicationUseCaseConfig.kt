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
    fun recommendationScoreService(recommendationScorePort: RecommendationScorePort): RecommendationScoreService {
        return RecommendationScoreService(recommendationScorePort)
    }

    @Bean
    fun laptopProfileService(
        laptopPort: CrawledLaptopPort,
        laptopProfilePort: CrawledLaptopProfilePort,
        laptopProfileFactory: LaptopProfileFactory,
        recommendationScoreService: RecommendationScoreService,
    ): LaptopProfileService {
        return LaptopProfileService(
            laptopPort = laptopPort,
            laptopProfilePort = laptopProfilePort,
            laptopProfileFactory = laptopProfileFactory,
            recommendationScoreService = recommendationScoreService,
        )
    }

    @Bean
    fun laptopPriceHistoryService(laptopPriceHistoryPort: LaptopPriceHistoryPort): LaptopPriceHistoryService {
        return LaptopPriceHistoryService(laptopPriceHistoryPort)
    }

    @Bean
    fun saveCrawledLaptopService(
        laptopPort: CrawledLaptopPort,
        laptopProfileService: LaptopProfileService,
        laptopPriceHistoryService: LaptopPriceHistoryService,
    ): SaveCrawledLaptopUseCase {
        return SaveCrawledLaptopService(
            laptopPort = laptopPort,
            laptopProfileService = laptopProfileService,
            laptopPriceHistoryService = laptopPriceHistoryService,
        )
    }

    @Bean
    fun trackCrawlerRunService(crawlerRunPort: CrawlerRunPort): TrackCrawlerRunUseCase {
        return TrackCrawlerRunService(crawlerRunPort)
    }

    @Bean
    fun crawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunLockService(crawlerRunLockPort)
    }
}
