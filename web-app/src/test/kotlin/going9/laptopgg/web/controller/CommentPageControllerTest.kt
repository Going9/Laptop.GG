package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.AddCommentCommand
import going9.laptopgg.application.comment.AddCommentUseCase
import going9.laptopgg.application.comment.CommentMutationResult
import going9.laptopgg.application.comment.DeleteCommentCommand
import going9.laptopgg.application.comment.DeleteCommentUseCase
import going9.laptopgg.application.comment.UpdateCommentUseCase
import going9.laptopgg.application.comment.UpdateCommentCommand
import going9.laptopgg.web.dto.request.CommentRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CommentPageControllerTest {
    private val addCommentUseCase = Mockito.mock(AddCommentUseCase::class.java)
    private val updateCommentUseCase = Mockito.mock(UpdateCommentUseCase::class.java)
    private val deleteCommentUseCase = Mockito.mock(DeleteCommentUseCase::class.java)
    private val controller = CommentPageController(
        addCommentUseCase = addCommentUseCase,
        updateCommentUseCase = updateCommentUseCase,
        deleteCommentUseCase = deleteCommentUseCase,
    )

    @Test
    fun `comment create redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "좋아요", passWord = "pw")

        val viewName = controller.addComment(request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(addCommentUseCase).add(
            AddCommentCommand(
                laptopId = 3L,
                author = "iggy",
                content = "좋아요",
                password = "pw",
            ),
        )
    }

    @Test
    fun `comment edit redirects to canonical laptop detail after service call`() {
        val request = CommentRequest(laptopId = 999L, author = "iggy", content = "수정", passWord = "pw")
        Mockito.`when`(
            updateCommentUseCase.update(
                7L,
                UpdateCommentCommand(password = "pw", content = "수정"),
            ),
        ).thenReturn(CommentMutationResult(laptopId = 3L))

        val viewName = controller.editComment(7L, request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(updateCommentUseCase).update(
            7L,
            UpdateCommentCommand(password = "pw", content = "수정"),
        )
    }

    @Test
    fun `comment delete redirects to canonical laptop detail after service call`() {
        val request = CommentRequest(laptopId = 999L, passWord = "pw")
        Mockito.`when`(
            deleteCommentUseCase.delete(
                7L,
                DeleteCommentCommand(password = "pw"),
            ),
        ).thenReturn(CommentMutationResult(laptopId = 3L))

        val viewName = controller.deleteComment(7L, request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(deleteCommentUseCase).delete(
            7L,
            DeleteCommentCommand(password = "pw"),
        )
    }
}
