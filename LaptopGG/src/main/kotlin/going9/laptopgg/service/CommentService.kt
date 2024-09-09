package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Comment
import going9.laptopgg.domain.repository.CommentRepository
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.CommentRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    val commentRepository: CommentRepository,
    val laptopRepository: LaptopRepository,
) {
    val encoder = BCryptPasswordEncoder(16)

    @Transactional
    fun saveComment(comment: Comment, request: CommentRequest) {
        val comment = Comment(
            laptop = laptopRepository.findLaptopById(request.laptopId)!!,
            author = request.author,
            content = request.content,
            passWord = encoder.encode(request.passWord),
        )
        commentRepository.save(comment)
    }

    @Transactional
    fun getAllComments(): List<Comment> {
        val comments = commentRepository.findAll()
        return comments
    }

    @Transactional
    fun updateComment(commentId: Long, request: CommentRequest) {
        val comment: Comment = commentRepository.findById(commentId).orElseThrow{
            IllegalArgumentException("$commentId 번 코멘트가 없습니다.")
        }

        if (encoder.matches(request.passWord, comment.passWord)) {
            comment.updateComment(request.author, request.content)
        } else throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
    }

    @Transactional
    fun deleteComment(commentId: Long) {
        val comment: Comment = commentRepository.findById(commentId).orElseThrow{
            IllegalArgumentException("$commentId 번 코멘트가 없습니다.")
        }
        commentRepository.delete(comment)
    }
}