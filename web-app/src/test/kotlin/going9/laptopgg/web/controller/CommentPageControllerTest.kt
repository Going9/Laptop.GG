package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.AddCommentCommand
import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.comment.UpdateCommentCommand
import going9.laptopgg.web.dto.request.CommentRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CommentPageControllerTest {
    private val manageCommentUseCase = Mockito.mock(ManageCommentUseCase::class.java)
    private val controller = CommentPageController(manageCommentUseCase)

    @Test
    fun `comment create redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "좋아요", passWord = "pw")

        val viewName = controller.addComment(request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(manageCommentUseCase).add(
            AddCommentCommand(
                laptopId = 3L,
                author = "iggy",
                content = "좋아요",
                password = "pw",
            ),
        )
    }

    @Test
    fun `comment edit redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "수정", passWord = "pw")

        val viewName = controller.editComment(7L, request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(manageCommentUseCase).update(
            7L,
            UpdateCommentCommand(password = "pw", content = "수정"),
        )
    }
}
