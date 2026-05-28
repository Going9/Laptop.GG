package going9.laptopgg.application.port.out

import going9.laptopgg.domain.laptop.Comment

interface CommentPort {
    fun findById(commentId: Long): Comment?
    fun findAllByLaptopId(laptopId: Long): List<Comment>
    fun save(comment: Comment): Comment
    fun delete(comment: Comment)
}
