package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.AddCommentUseCase
import going9.laptopgg.application.comment.DeleteCommentUseCase
import going9.laptopgg.application.comment.ListLaptopCommentsUseCase
import going9.laptopgg.application.comment.UpdateCommentUseCase
import going9.laptopgg.web.dto.request.CommentDeleteRequest
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.request.CommentUpdateRequest
import going9.laptopgg.web.dto.response.CommentResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
internal class CommentController(
    private val addCommentUseCase: AddCommentUseCase,
    private val listLaptopCommentsUseCase: ListLaptopCommentsUseCase,
    private val updateCommentUseCase: UpdateCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
) {

    @PostMapping
    fun saveComment(@RequestBody commentRequest: CommentRequest) {
        addCommentUseCase.add(commentRequest.toCommand())
    }

    @GetMapping
    fun getAllComments(@RequestParam laptopId: Long): List<CommentResponse> {
        return listLaptopCommentsUseCase.listByLaptop(laptopId).map(CommentResponse::from)
    }

    @PutMapping("/{commentId}/edit")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentUpdateRequest,
    ) {
        updateCommentUseCase.update(commentId, request.toCommand())
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentDeleteRequest,
    ) {
        deleteCommentUseCase.delete(commentId, request.toCommand())
    }
}
