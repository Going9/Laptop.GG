package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Comment
import going9.laptopgg.application.port.out.CommentPort
import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.dto.request.CommentDeleteRequest
import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.CommentUpdateRequest
import going9.laptopgg.dto.response.CommentResponse
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
    fun saveComment(request: CommentRequest) {
        val comment = Comment(
            laptop = laptopPort.findById(request.laptopId) ?: throw IllegalArgumentException("Laptop not found: ${request.laptopId}"),
            author = request.author,
            content = request.content,
            passWord = encoder.encode(request.passWord),
        )
        commentPort.save(comment)
    }

    @Transactional
    fun getAllComments(laptopId: Long): List<CommentResponse> {
        val comments = commentPort.findAllByLaptopId(laptopId).map {
            comment -> CommentResponse.of(comment)
        }
        return comments
    }

    @Transactional
    fun updateComment(commentId: Long, request: CommentUpdateRequest) {
        val comment: Comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
        validatePassword(comment, request.passWord)
        comment.updateComment(request.content)
    }

    @Transactional
    fun deleteComment(commentId: Long, request: CommentDeleteRequest) {
        val comment: Comment = commentPort.findById(commentId) ?: throw IllegalArgumentException("Comment not found: $commentId")
        validatePassword(comment, request.passWord)
        commentPort.delete(comment)
    }

    private fun validatePassword(comment: Comment, password: String) {
        require(encoder.matches(password, comment.passWord)) {
            "비밀번호가 일치하지 않습니다."
        }
    }
}
