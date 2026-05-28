package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentRecord
import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort

interface ManageCommentUseCase {
    fun add(command: AddCommentCommand)
    fun listByLaptop(laptopId: Long): List<CommentResult>
    fun update(commentId: Long, command: UpdateCommentCommand)
    fun delete(commentId: Long, command: DeleteCommentCommand)
}

internal class DefaultManageCommentUseCase(
    private val commentPort: CommentPort,
    private val laptopPort: CommentLaptopPort,
    private val passwordHashPort: PasswordHashPort,
    private val transactionPort: ApplicationTransactionPort,
) : ManageCommentUseCase {
    override fun add(command: AddCommentCommand) {
        validateAdd(command)
        transactionPort.write {
            addInTransaction(command)
        }
    }

    override fun listByLaptop(laptopId: Long): List<CommentResult> {
        validateLaptopId(laptopId)
        return transactionPort.read {
            validateLaptopExists(laptopId)
            commentPort.findAllByLaptopId(laptopId).map { comment ->
                CommentResult(
                    id = comment.id,
                    author = comment.author,
                    content = comment.content,
                )
            }
        }
    }

    override fun update(commentId: Long, command: UpdateCommentCommand) {
        validateCommentId(commentId)
        validateUpdate(command)
        transactionPort.write {
            val comment = commentPort.findById(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
            validatePassword(comment, command.password)
            commentPort.updateContent(commentId, command.content)
        }
    }

    override fun delete(commentId: Long, command: DeleteCommentCommand) {
        validateCommentId(commentId)
        validateDelete(command)
        transactionPort.write {
            val comment = commentPort.findById(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
            validatePassword(comment, command.password)
            commentPort.deleteById(commentId)
        }
    }

    private fun addInTransaction(command: AddCommentCommand) {
        validateLaptopExists(command.laptopId)
        commentPort.add(
            laptopId = command.laptopId,
            author = command.author,
            content = command.content,
            passwordHash = passwordHashPort.hash(command.password),
        )
    }

    private fun validateAdd(command: AddCommentCommand) {
        requirePositiveId(fieldName = "laptopId", value = command.laptopId)
        requireNonBlank(fieldName = "author", value = command.author)
        requireNonBlank(fieldName = "content", value = command.content)
        requireNonBlank(fieldName = "password", value = command.password)
    }

    private fun validateLaptopExists(laptopId: Long) {
        if (!laptopPort.existsById(laptopId)) {
            throw ResourceNotFoundException("Laptop", laptopId)
        }
    }

    private fun validateLaptopId(laptopId: Long) {
        requirePositiveId(fieldName = "laptopId", value = laptopId)
    }

    private fun validateCommentId(commentId: Long) {
        requirePositiveId(fieldName = "commentId", value = commentId)
    }

    private fun validateUpdate(command: UpdateCommentCommand) {
        requireNonBlank(fieldName = "content", value = command.content)
        requireNonBlank(fieldName = "password", value = command.password)
    }

    private fun validateDelete(command: DeleteCommentCommand) {
        requireNonBlank(fieldName = "password", value = command.password)
    }

    private fun requirePositiveId(fieldName: String, value: Long) {
        if (value <= 0) {
            throw InvalidCommandException("$fieldName must be positive.")
        }
    }

    private fun requireNonBlank(fieldName: String, value: String) {
        if (value.isBlank()) {
            throw InvalidCommandException("$fieldName must not be blank.")
        }
    }

    private fun validatePassword(comment: CommentRecord, password: String) {
        if (!passwordHashPort.matches(password, comment.passwordHash)) {
            throw AuthenticationFailedException("비밀번호가 일치하지 않습니다.")
        }
    }
}
