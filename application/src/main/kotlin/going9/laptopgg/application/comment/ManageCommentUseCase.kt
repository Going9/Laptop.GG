package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentMutationRecord
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort

interface ManageCommentUseCase {
    fun add(command: AddCommentCommand)
    fun listByLaptop(laptopId: Long): List<CommentResult>
    fun update(commentId: Long, command: UpdateCommentCommand): CommentMutationResult
    fun delete(commentId: Long, command: DeleteCommentCommand): CommentMutationResult
}

internal class DefaultManageCommentUseCase(
    private val commentQueryPort: CommentQueryPort,
    private val commentMutationPort: CommentMutationPort,
    private val laptopPort: CommentLaptopPort,
    private val passwordHashPort: PasswordHashPort,
    private val transactionPort: ApplicationTransactionPort,
) : ManageCommentUseCase {
    override fun add(command: AddCommentCommand) {
        validateAdd(command)
        val author = normalizeDisplayText(command.author)
        val content = normalizeDisplayText(command.content)
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

    override fun listByLaptop(laptopId: Long): List<CommentResult> {
        validateLaptopId(laptopId)
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

    override fun update(commentId: Long, command: UpdateCommentCommand): CommentMutationResult {
        validateCommentId(commentId)
        validateUpdate(command)
        val content = normalizeDisplayText(command.content)
        val comment = findCommentInReadTransaction(commentId)
        validatePassword(comment, command.password)
        return transactionPort.write {
            commentMutationPort.updateContent(commentId, content)
            CommentMutationResult(laptopId = comment.laptopId)
        }
    }

    override fun delete(commentId: Long, command: DeleteCommentCommand): CommentMutationResult {
        validateCommentId(commentId)
        validateDelete(command)
        val comment = findCommentInReadTransaction(commentId)
        validatePassword(comment, command.password)
        return transactionPort.write {
            commentMutationPort.deleteById(commentId)
            CommentMutationResult(laptopId = comment.laptopId)
        }
    }

    private fun findCommentInReadTransaction(commentId: Long): CommentMutationRecord {
        return transactionPort.read {
            commentMutationPort.findMutationById(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
        }
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

    private fun normalizeDisplayText(value: String): String {
        return value.trim()
    }

    private fun validatePassword(comment: CommentMutationRecord, password: String) {
        if (!passwordHashPort.matches(password, comment.passwordHash)) {
            throw AuthenticationFailedException("비밀번호가 일치하지 않습니다.")
        }
    }
}
