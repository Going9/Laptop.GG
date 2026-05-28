package going9.laptopgg.integration.config

import going9.laptopgg.application.recommendation.LaptopRecommendationResultMapper
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.RecommendationCandidateFilterFactory
import going9.laptopgg.application.recommendation.RecommendationReasonBuilder
import going9.laptopgg.application.recommendation.RecommendationScoreCalculator
import going9.laptopgg.application.recommendation.RecommendationSortModeResolver
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class IntegrationWebUseCaseConfig {
    @Bean
    fun recommendationReasonBuilder(): RecommendationReasonBuilder {
        return RecommendationReasonBuilder()
    }

    @Bean
    fun recommendationScoreCalculator(recommendationReasonBuilder: RecommendationReasonBuilder): RecommendationScoreCalculator {
        return RecommendationScoreCalculator(recommendationReasonBuilder)
    }

    @Bean
    fun recommendationCandidateFilterFactory(): RecommendationCandidateFilterFactory {
        return RecommendationCandidateFilterFactory()
    }

    @Bean
    fun recommendationSortModeResolver(): RecommendationSortModeResolver {
        return RecommendationSortModeResolver()
    }

    @Bean
    fun laptopRecommendationResultMapper(): LaptopRecommendationResultMapper {
        return LaptopRecommendationResultMapper()
    }

    @Bean
    fun recommendLaptopsUseCase(
        recommendationCandidatePort: RecommendationCandidatePort,
        recommendationScoreCalculator: RecommendationScoreCalculator,
        candidateFilterFactory: RecommendationCandidateFilterFactory,
        sortModeResolver: RecommendationSortModeResolver,
        resultMapper: LaptopRecommendationResultMapper,
    ): RecommendLaptopsUseCase {
        return RecommendLaptopsUseCase(
            recommendationCandidatePort = recommendationCandidatePort,
            recommendationScoreCalculator = recommendationScoreCalculator,
            candidateFilterFactory = candidateFilterFactory,
            sortModeResolver = sortModeResolver,
            resultMapper = resultMapper,
        )
    }
}
