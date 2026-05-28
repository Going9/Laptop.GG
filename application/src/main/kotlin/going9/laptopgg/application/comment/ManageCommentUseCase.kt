package going9.laptopgg.application.comment

import going9.laptopgg.application.port.out.CommentPort
import going9.laptopgg.application.port.out.CommentRecord
import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.application.port.out.PasswordHashPort
import org.springframework.transaction.annotation.Transactional

@Transactional
class ManageCommentUseCase(
    private val commentPort: CommentPort,
    private val laptopPort: LaptopPort,
    private val passwordHashPort: PasswordHashPort,
) {
    fun add(command: AddCommentCommand) {
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

    @Transactional(readOnly = true)
    fun listByLaptop(laptopId: Long): List<CommentResult> {
        return commentPort.findAllByLaptopId(laptopId).map { comment ->
            CommentResult(
                id = comment.id,
                author = comment.author,
                content = comment.content,
            )
        }
    }

    fun update(commentId: Long, command: UpdateCommentCommand) {
        val comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
        validatePassword(comment, command.password)
        commentPort.updateContent(commentId, command.content)
    }

    fun delete(commentId: Long, command: DeleteCommentCommand) {
        val comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
        validatePassword(comment, command.password)
        commentPort.deleteById(commentId)
    }

    private fun validatePassword(comment: CommentRecord, password: String) {
        require(passwordHashPort.matches(password, comment.passwordHash)) {
            "비밀번호가 일치하지 않습니다."
        }
    }
}
