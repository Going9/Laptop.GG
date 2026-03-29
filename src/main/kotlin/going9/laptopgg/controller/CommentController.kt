package going9.laptopgg.controller

import going9.laptopgg.domain.laptop.Comment
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
        println(commentRequest.toString())
        commentService.saveComment(commentRequest)
    }

    @GetMapping
    fun getAllComments(laptopId: Long): List<CommentResponse> {
        return commentService.getAllComments(laptopId)
    }

    @PutMapping("/{commentId}/edit")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: CommentRequest
    ) {
        commentService.updateComment(commentId, request)
    }

    @DeleteMapping
    fun deleteComment(@PathVariable commentId: Long) {
        commentService.deleteComment(commentId)
    }
}