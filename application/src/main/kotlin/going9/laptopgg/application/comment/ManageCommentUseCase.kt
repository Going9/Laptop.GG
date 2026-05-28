package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentRecord
import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.PasswordHashPort
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
        transactionPort.write {
            addInTransaction(command)
        }
    }

    override fun listByLaptop(laptopId: Long): List<CommentResult> {
        return transactionPort.read {
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
        transactionPort.write {
            val comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
            validatePassword(comment, command.password)
            commentPort.updateContent(commentId, command.content)
        }
    }

    override fun delete(commentId: Long, command: DeleteCommentCommand) {
        transactionPort.write {
            val comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
            validatePassword(comment, command.password)
            commentPort.deleteById(commentId)
        }
    }

    private fun addInTransaction(command: AddCommentCommand) {
        require(laptopPort.existsById(command.laptopId)) {
            "Laptop not found: ${command.laptopId}"
        }
        commentPort.add(
            laptopId = command.laptopId,
            author = command.author,
            content = command.content,
            passwordHash = passwordHashPort.hash(command.password),
        )
    }

    private fun validatePassword(comment: CommentRecord, password: String) {
        require(passwordHashPort.matches(password, comment.passwordHash)) {
            "비밀번호가 일치하지 않습니다."
        }
    }
}
