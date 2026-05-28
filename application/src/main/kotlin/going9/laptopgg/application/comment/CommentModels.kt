package going9.laptopgg.application.comment

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

data class CommentMutationResult(
    val laptopId: Long,
)

data class CommentResult(
    val id: Long,
    val author: String,
    val content: String,
)
