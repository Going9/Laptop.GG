package going9.laptopgg.infrastructure.jpa.repository.web

import going9.laptopgg.persistence.model.web.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
    fun findAllProjectedByLaptop_IdOrderByIdAsc(laptopId: Long): List<CommentListProjection>
}

interface CommentListProjection {
    val id: Long?
    val author: String
    val content: String
}
