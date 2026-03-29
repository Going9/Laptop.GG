package going9.laptopgg.dto.response

import going9.laptopgg.domain.laptop.Comment

data class CommentResponse(
    val id: Long?,
    val author: String,
    val content: String
) {
    companion object {
        fun of(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id,
                author = comment.author,
                content = comment.content
            )
        }
    }
}