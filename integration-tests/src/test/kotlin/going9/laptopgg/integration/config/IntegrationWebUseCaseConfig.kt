package going9.laptopgg.integration.config

import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.RecommendationScoreCalculator
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IntegrationWebUseCaseConfig {
    @Bean
    fun recommendationScoreCalculator(): RecommendationScoreCalculator {
        return RecommendationScoreCalculator()
    }

    @Bean
    fun recommendLaptopsUseCase(
        recommendationCandidatePort: RecommendationCandidatePort,
        recommendationScoreCalculator: RecommendationScoreCalculator,
    ): RecommendLaptopsUseCase {
        return RecommendLaptopsUseCase(
            recommendationCandidatePort = recommendationCandidatePort,
            recommendationScoreCalculator = recommendationScoreCalculator,
        )
    }
}
