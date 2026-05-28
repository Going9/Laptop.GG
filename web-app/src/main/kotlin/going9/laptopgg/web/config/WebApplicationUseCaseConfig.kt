package going9.laptopgg.web.config

import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.laptop.port.LaptopPort
import going9.laptopgg.application.recommendation.LaptopRecommendationResultMapper
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.RecommendationCandidateFilterFactory
import going9.laptopgg.application.recommendation.RecommendationScoreCalculator
import going9.laptopgg.application.recommendation.RecommendationSortModeResolver
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class WebApplicationUseCaseConfig {
    @Bean
    fun manageCommentUseCase(
        commentPort: CommentPort,
        laptopPort: CommentLaptopPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): ManageCommentUseCase {
        return ManageCommentUseCase(
            commentPort = commentPort,
            laptopPort = laptopPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun getLaptopDetailUseCase(
        laptopPort: LaptopPort,
        transactionPort: ApplicationTransactionPort,
    ): GetLaptopDetailUseCase {
        return GetLaptopDetailUseCase(
            laptopPort = laptopPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun recommendationScoreCalculator(): RecommendationScoreCalculator {
        return RecommendationScoreCalculator()
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
