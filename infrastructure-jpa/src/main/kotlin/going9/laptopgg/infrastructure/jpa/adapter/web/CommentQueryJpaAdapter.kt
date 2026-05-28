package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.comment.port.CommentListRecord
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.infrastructure.jpa.repository.web.CommentListProjection
import going9.laptopgg.infrastructure.jpa.repository.web.CommentRepository
import org.springframework.stereotype.Component

@Component
internal class CommentQueryJpaAdapter(
    private val commentRepository: CommentRepository,
) : CommentQueryPort {
    override fun findAllByLaptopId(laptopId: Long): List<CommentListRecord> {
        return commentRepository.findAllProjectedByLaptop_IdOrderByIdAsc(laptopId).map { it.toListRecord() }
    }

    private fun CommentListProjection.toListRecord(): CommentListRecord {
        return CommentListRecord(
            id = id ?: throw ApplicationInvalidStateException("Persisted comment id must not be null."),
            author = author,
            content = content,
        )
    }
}
