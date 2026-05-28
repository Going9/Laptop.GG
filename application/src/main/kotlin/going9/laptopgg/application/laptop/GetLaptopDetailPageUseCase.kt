package going9.laptopgg.application.laptop

import going9.laptopgg.application.comment.CommentResult
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.port.LaptopPort

interface GetLaptopDetailPageUseCase {
    fun get(laptopId: Long): LaptopDetailPageResult
}

internal class DefaultGetLaptopDetailPageUseCase(
    private val laptopPort: LaptopPort,
    private val commentPort: CommentPort,
    private val transactionPort: ApplicationTransactionPort,
) : GetLaptopDetailPageUseCase {
    override fun get(laptopId: Long): LaptopDetailPageResult {
        validateLaptopId(laptopId)
        return transactionPort.read {
            val laptop = laptopPort.findDetailById(laptopId) ?: throw ResourceNotFoundException("Laptop", laptopId)
            LaptopDetailPageResult(
                laptopDetail = laptop.toLaptopDetailResult(),
                comments = commentPort.findAllByLaptopId(laptopId).map { comment ->
                    CommentResult(
                        id = comment.id,
                        author = comment.author,
                        content = comment.content,
                    )
                },
            )
        }
    }

    private fun validateLaptopId(laptopId: Long) {
        if (laptopId <= 0) {
            throw InvalidCommandException("laptopId must be positive.")
        }
    }
}

data class LaptopDetailPageResult(
    val laptopDetail: LaptopDetailResult,
    val comments: List<CommentResult>,
)
