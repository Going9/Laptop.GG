package going9.laptopgg.web.dto.request

import going9.laptopgg.application.comment.AddCommentCommand

data class CommentRequest(
    val laptopId: Long = 0,
    val author: String = "",
    val content: String = "",
    val passWord: String = "",
) {
    fun toCommand(): AddCommentCommand {
        return AddCommentCommand(
            laptopId = laptopId,
            author = author,
            content = content,
            password = passWord,
        )
    }
}
