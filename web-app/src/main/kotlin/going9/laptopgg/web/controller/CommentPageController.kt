package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.AddCommentUseCase
import going9.laptopgg.application.comment.DeleteCommentUseCase
import going9.laptopgg.application.comment.UpdateCommentUseCase
import going9.laptopgg.web.dto.request.CommentDeleteRequest
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.request.CommentUpdateRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

@Controller
internal class CommentPageController(
    private val addCommentUseCase: AddCommentUseCase,
    private val updateCommentUseCase: UpdateCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
) {
    @PostMapping("/comments")
    fun addComment(@ModelAttribute commentRequest: CommentRequest): String {
        addCommentUseCase.add(commentRequest.toCommand())
        return "redirect:/laptops/${commentRequest.laptopId}"
    }

    @PostMapping("/comments/{commentId}/edit")
    fun editComment(
        @PathVariable commentId: Long,
        @ModelAttribute commentRequest: CommentRequest,
    ): String {
        val result = updateCommentUseCase.update(
            commentId,
            CommentUpdateRequest(
                passWord = commentRequest.passWord,
                content = commentRequest.content,
            ).toCommand(),
        )
        return "redirect:/laptops/${result.laptopId}"
    }

    @PostMapping("/comments/{commentId}/delete")
    fun deleteComment(
        @PathVariable commentId: Long,
        @ModelAttribute commentRequest: CommentRequest,
    ): String {
        val result = deleteCommentUseCase.delete(
            commentId,
            CommentDeleteRequest(passWord = commentRequest.passWord).toCommand(),
        )
        return "redirect:/laptops/${result.laptopId}"
    }
}
