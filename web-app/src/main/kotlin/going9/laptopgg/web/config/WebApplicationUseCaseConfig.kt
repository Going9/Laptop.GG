package going9.laptopgg.web.config

import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.port.out.CommentPort
import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.application.port.out.LaptopProfilePort
import going9.laptopgg.application.port.out.PasswordHashPort
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.service.ScoreCalculatorService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class WebApplicationUseCaseConfig {
    @Bean
    fun manageCommentUseCase(
        commentPort: CommentPort,
        laptopPort: LaptopPort,
        passwordHashPort: PasswordHashPort,
    ): ManageCommentUseCase {
        return ManageCommentUseCase(
            commentPort = commentPort,
            laptopPort = laptopPort,
            passwordHashPort = passwordHashPort,
        )
    }

    @Bean
    fun getLaptopDetailUseCase(laptopPort: LaptopPort): GetLaptopDetailUseCase {
        return GetLaptopDetailUseCase(laptopPort)
    }

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
}
