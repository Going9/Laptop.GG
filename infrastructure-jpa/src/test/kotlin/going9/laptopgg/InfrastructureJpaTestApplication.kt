package going9.laptopgg

import going9.laptopgg.application.crawler.CpuClassifier
import going9.laptopgg.application.crawler.GpuClassifier
import going9.laptopgg.application.crawler.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.LaptopProfileFactory
import going9.laptopgg.application.crawler.LaptopProfileService
import going9.laptopgg.application.crawler.ProfileScorePolicy
import going9.laptopgg.application.crawler.RecommendationScoreService
import going9.laptopgg.application.crawler.SaveCrawledLaptopService
import going9.laptopgg.application.crawler.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.port.out.CrawledLaptopPort
import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.port.out.RecommendationScorePort
import going9.laptopgg.application.port.out.LaptopProfilePort
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.service.ScoreCalculatorService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.infrastructure.jpa.adapter.shared",
        "going9.laptopgg.infrastructure.jpa.adapter.web",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.repository.crawler",
        "going9.laptopgg.infrastructure.jpa.repository.shared",
        "going9.laptopgg.infrastructure.jpa.repository.web",
    ],
)
class InfrastructureJpaTestApplication {
    @Bean
    fun scoreCalculatorService(): ScoreCalculatorService {
        return ScoreCalculatorService()
    }

    @Bean
    fun recommendLaptopsUseCase(
        laptopProfilePort: LaptopProfilePort,
        scoreCalculatorService: ScoreCalculatorService,
    ): RecommendLaptopsUseCase {
        return RecommendLaptopsUseCase(
            laptopProfilePort = laptopProfilePort,
            scoreCalculatorService = scoreCalculatorService,
        )
    }

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
}
