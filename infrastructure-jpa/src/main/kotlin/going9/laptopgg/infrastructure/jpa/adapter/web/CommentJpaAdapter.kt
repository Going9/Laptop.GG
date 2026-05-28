package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.comment.port.CommentListRecord
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.CommentRecord
import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.infrastructure.jpa.repository.web.CommentListProjection
import going9.laptopgg.infrastructure.jpa.repository.web.CommentRepository
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopRepository
import going9.laptopgg.persistence.model.web.Comment
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class CommentJpaAdapter(
    private val commentRepository: CommentRepository,
    private val laptopRepository: WebLaptopRepository,
) : CommentPort {
    override fun findById(commentId: Long): CommentRecord? {
        return commentRepository.findByIdOrNull(commentId)?.toRecord()
    }

    override fun findAllByLaptopId(laptopId: Long): List<CommentListRecord> {
        return commentRepository.findAllProjectedByLaptop_IdOrderByIdAsc(laptopId).map { it.toListRecord() }
    }

    override fun add(laptopId: Long, author: String, content: String, passwordHash: String) {
        val laptop = laptopRepository.findByIdOrNull(laptopId)
            ?: throw ResourceNotFoundException("Laptop", laptopId)
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
            ?: throw ResourceNotFoundException("Comment", commentId)
        comment.updateComment(content)
    }

    override fun deleteById(commentId: Long) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw ResourceNotFoundException("Comment", commentId)
        commentRepository.delete(comment)
    }

    private fun Comment.toRecord(): CommentRecord {
        val commentId = id ?: throw ApplicationInvalidStateException("Persisted comment id must not be null.")
        val laptopId = laptop.id ?: throw ApplicationInvalidStateException("Persisted comment laptop id must not be null.")
        return CommentRecord(
            id = commentId,
            laptopId = laptopId,
            author = author,
            content = content,
            passwordHash = passWord,
        )
    }

    private fun CommentListProjection.toListRecord(): CommentListRecord {
        return CommentListRecord(
            id = id ?: throw ApplicationInvalidStateException("Persisted comment id must not be null."),
            author = author,
            content = content,
        )
    }
}
