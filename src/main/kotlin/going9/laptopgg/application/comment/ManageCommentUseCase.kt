package going9.laptopgg.application.comment

import going9.laptopgg.dto.request.CommentDeleteRequest
import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.CommentUpdateRequest
import going9.laptopgg.dto.response.CommentResponse
import going9.laptopgg.service.CommentService
import org.springframework.stereotype.Service

@Service
class ManageCommentUseCase(
    private val commentService: CommentService,
) {
    fun add(request: CommentRequest) {
        commentService.saveComment(request)
    }

    fun listByLaptop(laptopId: Long): List<CommentResponse> {
        return commentService.getAllComments(laptopId)
    }

    fun update(commentId: Long, request: CommentUpdateRequest) {
        commentService.updateComment(commentId, request)
    }

    fun delete(commentId: Long, request: CommentDeleteRequest) {
        commentService.deleteComment(commentId, request)
    }
}
