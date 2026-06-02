package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort

interface ListLaptopCommentsUseCase {
    fun listByLaptop(laptopId: Long): List<CommentResult>
}

internal class DefaultListLaptopCommentsUseCase(
    private val commentQueryPort: CommentQueryPort,
    private val laptopPort: CommentLaptopPort,
    private val transactionPort: ApplicationTransactionPort,
) : ListLaptopCommentsUseCase {
    override fun listByLaptop(laptopId: Long): List<CommentResult> {
        CommentCommandValidator.validateLaptopId(laptopId)
        return transactionPort.read {
            validateLaptopExists(laptopId)
            commentQueryPort.findAllByLaptopId(laptopId).map { comment ->
                CommentResult(
                    id = comment.id,
                    author = comment.author,
                    content = comment.content,
                )
            }
        }
    }

    private fun validateLaptopExists(laptopId: Long) {
        if (!laptopPort.existsById(laptopId)) {
            throw ResourceNotFoundException("Laptop", laptopId)
        }
    }
}
