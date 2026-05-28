package going9.laptopgg.web.config

import going9.laptopgg.application.comment.CommentUseCaseAssembler
import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.laptop.LaptopUseCaseAssembler
import going9.laptopgg.application.laptop.port.LaptopPort
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.RecommendationUseCaseAssembler
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class WebApplicationUseCaseConfig {
    @Bean
    fun manageCommentUseCase(
        commentPort: CommentPort,
        laptopPort: CommentLaptopPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): ManageCommentUseCase {
        return CommentUseCaseAssembler.createManageCommentUseCase(
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
        return LaptopUseCaseAssembler.createGetLaptopDetailUseCase(
            laptopPort = laptopPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun recommendLaptopsUseCase(
        recommendationCandidatePort: RecommendationCandidatePort,
        transactionPort: ApplicationTransactionPort,
    ): RecommendLaptopsUseCase {
        return RecommendationUseCaseAssembler.createRecommendLaptopsUseCase(
            recommendationCandidatePort = recommendationCandidatePort,
            transactionPort = transactionPort,
        )
    }
}
