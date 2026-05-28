package going9.laptopgg.dto.request

import going9.laptopgg.application.comment.DeleteCommentCommand

data class CommentDeleteRequest(
    val passWord: String = "",
) {
    fun toCommand(): DeleteCommentCommand {
        return DeleteCommentCommand(password = passWord)
    }
}
