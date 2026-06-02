package going9.laptopgg.web.dto.request

import going9.laptopgg.application.comment.UpdateCommentCommand

data class CommentUpdateRequest(
    val passWord: String,
    val content: String,
) {
    fun toCommand(): UpdateCommentCommand {
        return UpdateCommentCommand(
            password = passWord,
            content = content,
        )
    }
}
