package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Comment
import going9.laptopgg.domain.repository.CommentRepository
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.CommentDeleteRequest
import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.CommentUpdateRequest
import going9.laptopgg.dto.response.CommentResponse
import going9.laptopgg.util.findByOrThrow
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    val commentRepository: CommentRepository,
    val laptopRepository: LaptopRepository,
) {
    private val encoder = BCryptPasswordEncoder(4)

    @Transactional
    fun saveComment(request: CommentRequest) {
        val comment = Comment(
            laptop = laptopRepository.findByOrThrow(request.laptopId),
            author = request.author,
            content = request.content,
            passWord = encoder.encode(request.passWord),
        )
        commentRepository.save(comment)
    }

    @Transactional
    fun getAllComments(laptopId: Long): List<CommentResponse> {
        val comments = commentRepository.findAllByLaptop_Id(laptopId).map {
            comment -> CommentResponse.of(comment)
        }
        return comments
    }

    @Transactional
    fun updateComment(commentId: Long, request: CommentUpdateRequest) {
        val comment: Comment = commentRepository.findByOrThrow(commentId)
        validatePassword(comment, request.passWord)
        comment.updateComment(request.content)
    }

    @Transactional
    fun deleteComment(commentId: Long, request: CommentDeleteRequest) {
        val comment: Comment = commentRepository.findByOrThrow(commentId)
        validatePassword(comment, request.passWord)
        commentRepository.delete(comment)
    }

    private fun validatePassword(comment: Comment, password: String) {
        require(encoder.matches(password, comment.passWord)) {
            "비밀번호가 일치하지 않습니다."
        }
    }
}
