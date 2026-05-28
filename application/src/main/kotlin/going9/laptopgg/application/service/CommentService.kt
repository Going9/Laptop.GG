package going9.laptopgg.application.service

import going9.laptopgg.application.comment.AddCommentCommand
import going9.laptopgg.application.comment.CommentResult
import going9.laptopgg.application.comment.DeleteCommentCommand
import going9.laptopgg.application.comment.UpdateCommentCommand
import going9.laptopgg.application.port.out.CommentPort
import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.domain.laptop.Comment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentPort: CommentPort,
    private val laptopPort: LaptopPort,
) {
    private val encoder = BCryptPasswordEncoder(4)

    @Transactional
    fun saveComment(command: AddCommentCommand) {
        val comment = Comment(
            laptop = laptopPort.findById(command.laptopId) ?: throw IllegalArgumentException("Laptop not found: ${command.laptopId}"),
            author = command.author,
            content = command.content,
            passWord = encoder.encode(command.password),
        )
        commentPort.save(comment)
    }

    @Transactional
    fun getAllComments(laptopId: Long): List<CommentResult> {
        val comments = commentPort.findAllByLaptopId(laptopId).map {
            comment -> CommentResult.from(comment)
        }
        return comments
    }

    @Transactional
    fun updateComment(commentId: Long, command: UpdateCommentCommand) {
        val comment: Comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
        validatePassword(comment, command.password)
        comment.updateComment(command.content)
    }

    @Transactional
    fun deleteComment(commentId: Long, command: DeleteCommentCommand) {
        val comment: Comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
        validatePassword(comment, command.password)
        commentPort.delete(comment)
    }

    private fun validatePassword(comment: Comment, password: String) {
        require(encoder.matches(password, comment.passWord)) {
            "비밀번호가 일치하지 않습니다."
        }
    }
}
