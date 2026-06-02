package going9.laptopgg.web.dto.response

import going9.laptopgg.application.comment.CommentResult

data class CommentResponse(
    val id: Long,
    val author: String,
    val content: String,
) {
    companion object {
        fun from(result: CommentResult): CommentResponse {
            return CommentResponse(
                id = result.id,
                author = result.author,
                content = result.content,
            )
        }
    }
}
