package going9.laptopgg.web.config

import going9.laptopgg.application.comment.AddCommentUseCase
import going9.laptopgg.application.comment.CommentUseCaseAssembler
import going9.laptopgg.application.comment.DeleteCommentUseCase
import going9.laptopgg.application.comment.ListLaptopCommentsUseCase
import going9.laptopgg.application.comment.UpdateCommentUseCase
import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailPageUseCase
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
    fun addCommentUseCase(
        commentMutationPort: CommentMutationPort,
        laptopPort: CommentLaptopPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): AddCommentUseCase {
        return CommentUseCaseAssembler.createAddCommentUseCase(
            commentMutationPort = commentMutationPort,
            laptopPort = laptopPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun listLaptopCommentsUseCase(
        commentQueryPort: CommentQueryPort,
        laptopPort: CommentLaptopPort,
        transactionPort: ApplicationTransactionPort,
    ): ListLaptopCommentsUseCase {
        return CommentUseCaseAssembler.createListLaptopCommentsUseCase(
            commentQueryPort = commentQueryPort,
            laptopPort = laptopPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun updateCommentUseCase(
        commentMutationPort: CommentMutationPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): UpdateCommentUseCase {
        return CommentUseCaseAssembler.createUpdateCommentUseCase(
            commentMutationPort = commentMutationPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun deleteCommentUseCase(
        commentMutationPort: CommentMutationPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): DeleteCommentUseCase {
        return CommentUseCaseAssembler.createDeleteCommentUseCase(
            commentMutationPort = commentMutationPort,
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
    fun getLaptopDetailPageUseCase(
        laptopPort: LaptopPort,
        commentQueryPort: CommentQueryPort,
        transactionPort: ApplicationTransactionPort,
    ): GetLaptopDetailPageUseCase {
        return LaptopUseCaseAssembler.createGetLaptopDetailPageUseCase(
            laptopPort = laptopPort,
            commentQueryPort = commentQueryPort,
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
