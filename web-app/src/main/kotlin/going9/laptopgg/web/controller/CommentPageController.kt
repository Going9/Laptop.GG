package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.request.CommentUpdateRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

@Controller
internal class CommentPageController(
    private val manageCommentUseCase: ManageCommentUseCase,
) {
    @PostMapping("/comments")
    fun addComment(@ModelAttribute commentRequest: CommentRequest): String {
        manageCommentUseCase.add(commentRequest.toCommand())
        return "redirect:/laptops/${commentRequest.laptopId}"
    }

    @PostMapping("/comments/{commentId}/edit")
    fun editComment(
        @PathVariable commentId: Long,
        @ModelAttribute commentRequest: CommentRequest,
    ): String {
        manageCommentUseCase.update(
            commentId,
            CommentUpdateRequest(
                passWord = commentRequest.passWord,
                content = commentRequest.content,
            ).toCommand(),
        )
        return "redirect:/laptops/${commentRequest.laptopId}"
    }
}
