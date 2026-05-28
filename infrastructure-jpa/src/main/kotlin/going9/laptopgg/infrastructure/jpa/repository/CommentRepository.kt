package going9.laptopgg.infrastructure.jpa.repository

import going9.laptopgg.domain.laptop.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
    fun findAllByLaptop_Id(laptopId: Long): List<Comment>
}
