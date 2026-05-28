package going9.laptopgg.integration.config

import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.RecommendationUseCaseAssembler
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IntegrationWebUseCaseConfig {
    @Bean
    fun recommendLaptopsUseCase(
        recommendationCandidatePort: RecommendationCandidatePort,
    ): RecommendLaptopsUseCase {
        return RecommendationUseCaseAssembler.createRecommendLaptopsUseCase(recommendationCandidatePort)
    }
}
