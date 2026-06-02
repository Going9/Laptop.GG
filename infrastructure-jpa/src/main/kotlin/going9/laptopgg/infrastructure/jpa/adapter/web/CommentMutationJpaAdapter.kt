package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentMutationRecord
import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.infrastructure.jpa.repository.web.CommentMutationProjection
import going9.laptopgg.infrastructure.jpa.repository.web.CommentRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.web.Comment
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
internal class CommentMutationJpaAdapter(
    private val commentRepository: CommentRepository,
    private val entityManager: EntityManager,
) : CommentMutationPort {
    override fun findMutationById(commentId: Long): CommentMutationRecord? {
        return commentRepository.findMutationProjectedById(commentId)?.toMutationRecord()
    }

    override fun add(laptopId: Long, author: String, content: String, passwordHash: String) {
        val laptop = entityManager.getReference(Laptop::class.java, laptopId)
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
}
