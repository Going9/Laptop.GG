package going9.laptopgg.application.comment.port

interface CommentQueryPort {
    fun findAllByLaptopId(laptopId: Long): List<CommentListRecord>
}

data class CommentListRecord(
    val id: Long,
    val author: String,
    val content: String,
)
