package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.port.out.CommentPort
import going9.laptopgg.application.port.out.CommentRecord
import going9.laptopgg.persistence.model.laptop.Comment
import going9.laptopgg.infrastructure.jpa.repository.web.CommentRepository
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CommentJpaAdapter(
    private val commentRepository: CommentRepository,
    private val laptopRepository: WebLaptopRepository,
) : CommentPort {
    override fun findById(commentId: Long): CommentRecord? {
        return commentRepository.findByIdOrNull(commentId)?.toRecord()
    }

    override fun findAllByLaptopId(laptopId: Long): List<CommentRecord> {
        return commentRepository.findAllByLaptop_Id(laptopId).map { it.toRecord() }
    }

    override fun add(laptopId: Long, author: String, content: String, passwordHash: String) {
        val laptop = laptopRepository.findByIdOrNull(laptopId)
            ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        commentRepository.save(
            Comment(
                laptop = laptop,
                author = author,
                content = content,
                passWord = passwordHash,
            ),
        )
    }

    override fun updateContent(commentId: Long, content: String) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("Comment not found: $commentId")
        comment.updateComment(content)
    }

    override fun deleteById(commentId: Long) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("Comment not found: $commentId")
        commentRepository.delete(comment)
    }

    private fun Comment.toRecord(): CommentRecord {
        return CommentRecord(
            id = id,
            author = author,
            content = content,
            passwordHash = passWord,
        )
    }
}
