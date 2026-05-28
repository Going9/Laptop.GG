package going9.laptopgg.application.comment.port

interface CommentPort {
    fun findById(commentId: Long): CommentRecord?
    fun findAllByLaptopId(laptopId: Long): List<CommentListRecord>
    fun add(laptopId: Long, author: String, content: String, passwordHash: String)
    fun updateContent(commentId: Long, content: String)
    fun deleteById(commentId: Long)
}

data class CommentListRecord(
    val id: Long,
    val author: String,
    val content: String,
)

data class CommentRecord(
    val id: Long,
    val laptopId: Long,
    val author: String,
    val content: String,
    val passwordHash: String,
)
