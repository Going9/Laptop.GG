package going9.laptopgg.web.config

import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.GetLaptopDetailPageUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.laptop.LaptopUseCaseAssembler
import going9.laptopgg.application.laptop.port.LaptopPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class WebLaptopUseCaseConfig {
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
}
