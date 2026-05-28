package going9.laptopgg.infrastructure.jpa.adapter

import going9.laptopgg.application.port.out.CommentPort
import going9.laptopgg.domain.laptop.Comment
import going9.laptopgg.infrastructure.jpa.repository.CommentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CommentJpaAdapter(
    private val commentRepository: CommentRepository,
) : CommentPort {
    override fun findById(commentId: Long): Comment? {
        return commentRepository.findByIdOrNull(commentId)
    }

    override fun findAllByLaptopId(laptopId: Long): List<Comment> {
        return commentRepository.findAllByLaptop_Id(laptopId)
    }

    override fun save(comment: Comment): Comment {
        return commentRepository.save(comment)
    }

    override fun delete(comment: Comment) {
        commentRepository.delete(comment)
    }
}
