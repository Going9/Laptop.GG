package going9.laptopgg.application.comment

import going9.laptopgg.application.service.CommentService
import org.springframework.stereotype.Service

@Service
class ManageCommentUseCase(
    private val commentService: CommentService,
) {
    fun add(command: AddCommentCommand) {
        commentService.saveComment(command)
    }

    fun listByLaptop(laptopId: Long): List<CommentResult> {
        return commentService.getAllComments(laptopId)
    }

    fun update(commentId: Long, command: UpdateCommentCommand) {
        commentService.updateComment(commentId, command)
    }

    fun delete(commentId: Long, command: DeleteCommentCommand) {
        commentService.deleteComment(commentId, command)
    }
}
