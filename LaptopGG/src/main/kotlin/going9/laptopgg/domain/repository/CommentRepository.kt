package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Comment
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {

    fun findAllByLaptop_Id(laptopId: Long): List<Comment>
}