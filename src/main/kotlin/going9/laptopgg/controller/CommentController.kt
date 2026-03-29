package going9.laptopgg.controller

import going9.laptopgg.dto.request.CommentDeleteRequest
import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.CommentUpdateRequest
import going9.laptopgg.dto.response.CommentResponse
import going9.laptopgg.service.CommentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService,
) {

    @PostMapping
    fun saveComment(@RequestBody commentRequest: CommentRequest) {
        commentService.saveComment(commentRequest)
    }

    @GetMapping
    fun getAllComments(@RequestParam laptopId: Long): List<CommentResponse> {
        return commentService.getAllComments(laptopId)
    }

    @PutMapping("/{commentId}/edit")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentUpdateRequest,
    ) {
        commentService.updateComment(commentId, request)
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentDeleteRequest,
    ) {
        commentService.deleteComment(commentId, request)
    }
}
