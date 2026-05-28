package going9.laptopgg.application.comment

import going9.laptopgg.domain.laptop.Comment

data class AddCommentCommand(
    val laptopId: Long,
    val author: String,
    val content: String,
    val password: String,
)

data class UpdateCommentCommand(
    val password: String,
    val content: String,
)

data class DeleteCommentCommand(
    val password: String,
)

data class CommentResult(
    val id: Long?,
    val author: String,
    val content: String,
) {
    companion object {
        fun from(comment: Comment): CommentResult {
            return CommentResult(
                id = comment.id,
                author = comment.author,
                content = comment.content,
            )
        }
    }
}
