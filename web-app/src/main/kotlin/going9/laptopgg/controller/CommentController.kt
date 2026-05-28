package going9.laptopgg.controller

import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.dto.request.CommentDeleteRequest
import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.CommentUpdateRequest
import going9.laptopgg.dto.response.CommentResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val manageCommentUseCase: ManageCommentUseCase,
) {

    @PostMapping
    fun saveComment(@RequestBody commentRequest: CommentRequest) {
        manageCommentUseCase.add(commentRequest.toCommand())
    }

    @GetMapping
    fun getAllComments(@RequestParam laptopId: Long): List<CommentResponse> {
        return manageCommentUseCase.listByLaptop(laptopId).map(CommentResponse::from)
    }

    @PutMapping("/{commentId}/edit")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentUpdateRequest,
    ) {
        manageCommentUseCase.update(commentId, request.toCommand())
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentDeleteRequest,
    ) {
        manageCommentUseCase.delete(commentId, request.toCommand())
    }
}
