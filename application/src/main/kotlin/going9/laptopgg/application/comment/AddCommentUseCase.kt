package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort

interface AddCommentUseCase {
    fun add(command: AddCommentCommand)
}

internal class DefaultAddCommentUseCase(
    private val commentMutationPort: CommentMutationPort,
    private val laptopPort: CommentLaptopPort,
    private val passwordHashPort: PasswordHashPort,
    private val transactionPort: ApplicationTransactionPort,
) : AddCommentUseCase {
    override fun add(command: AddCommentCommand) {
        CommentCommandValidator.validateAdd(command)
        val author = CommentCommandValidator.normalizeDisplayText(command.author)
        val content = CommentCommandValidator.normalizeDisplayText(command.content)
        transactionPort.read {
            validateLaptopExists(command.laptopId)
        }
        val passwordHash = passwordHashPort.hash(command.password)
        transactionPort.write {
            commentMutationPort.add(
                laptopId = command.laptopId,
                author = author,
                content = content,
                passwordHash = passwordHash,
            )
        }
    }

    private fun validateLaptopExists(laptopId: Long) {
        if (!laptopPort.existsById(laptopId)) {
            throw ResourceNotFoundException("Laptop", laptopId)
        }
    }
}
