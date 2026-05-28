package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.comment.port.CommentListRecord
import going9.laptopgg.application.comment.port.CommentMutationRecord
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.infrastructure.jpa.repository.web.CommentListProjection
import going9.laptopgg.infrastructure.jpa.repository.web.CommentMutationProjection
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
    override fun findMutationById(commentId: Long): CommentMutationRecord? {
        return commentRepository.findMutationProjectedById(commentId)?.toMutationRecord()
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
        if (commentRepository.updateContentById(commentId, content) == 0) {
            throw ResourceNotFoundException("Comment", commentId)
        }
    }

    override fun deleteById(commentId: Long) {
        if (commentRepository.deleteByCommentId(commentId) == 0) {
            throw ResourceNotFoundException("Comment", commentId)
        }
    }

    private fun CommentMutationProjection.toMutationRecord(): CommentMutationRecord {
        return CommentMutationRecord(
            id = id ?: throw ApplicationInvalidStateException("Persisted comment id must not be null."),
            laptopId = laptopId ?: throw ApplicationInvalidStateException("Persisted comment laptop id must not be null."),
            passwordHash = passwordHash,
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
