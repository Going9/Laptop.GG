package going9.laptopgg.application.laptop

import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.port.LaptopPort

object LaptopUseCaseAssembler {
    fun createGetLaptopDetailUseCase(
        laptopPort: LaptopPort,
        transactionPort: ApplicationTransactionPort,
    ): GetLaptopDetailUseCase {
        return DefaultGetLaptopDetailUseCase(
            laptopPort = laptopPort,
            transactionPort = transactionPort,
        )
    }

    fun createGetLaptopDetailPageUseCase(
        laptopPort: LaptopPort,
        commentQueryPort: CommentQueryPort,
        transactionPort: ApplicationTransactionPort,
    ): GetLaptopDetailPageUseCase {
        return DefaultGetLaptopDetailPageUseCase(
            laptopPort = laptopPort,
            commentQueryPort = commentQueryPort,
            transactionPort = transactionPort,
        )
    }
}
