package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Comment
import going9.laptopgg.domain.repository.CommentRepository
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.CommentRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    val commentRepository: CommentRepository,
    val laptopRepository: LaptopRepository,
) {
    @Transactional
    fun saveComment(comment: Comment, request: CommentRequest) {
        val comment = Comment(
            laptop = laptopRepository.findLaptopById(request.laptopId)!!,
            author = request.author,
            content = request.content,
            passWord = request.passWord,
        )
        commentRepository.save(comment)
    }

    @Transactional
    fun getAllComments(): List<Comment> {
        val comments = commentRepository.findAll()
        return comments
    }

//    @Transactional
//    fun updateComments(passWord: String, commentId: Long): Comment {
//        val comment = commentRepository.findById(commentId)
//    }
}