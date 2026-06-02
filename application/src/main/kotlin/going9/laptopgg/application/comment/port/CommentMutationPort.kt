package going9.laptopgg.application.comment.port

interface CommentMutationPort {
    fun findMutationById(commentId: Long): CommentMutationRecord?
    fun add(laptopId: Long, author: String, content: String, passwordHash: String)
    fun updateContent(commentId: Long, content: String)
    fun deleteById(commentId: Long)
}

data class CommentMutationRecord(
    val id: Long,
    val laptopId: Long,
    val passwordHash: String,
)
