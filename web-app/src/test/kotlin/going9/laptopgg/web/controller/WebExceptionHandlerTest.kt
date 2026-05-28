package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationInvalidStateException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class WebExceptionHandlerTest {
    private val apiHandler = ApiExceptionHandler()
    private val pageHandler = PageExceptionHandler()

    @Test
    fun `web api maps invalid application state to 500 response`() {
        val entity = apiHandler.handleApplicationException(
            ApplicationInvalidStateException("invalid projection"),
        )

        assertThat(entity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(entity.body).isEqualTo(
            WebErrorResponse(
                code = "internal_error",
                message = "잠시 뒤 다시 시도해 주세요.",
            ),
        )
    }

    @Test
    fun `web api maps malformed framework requests to 400 response`() {
        val entity = apiHandler.handleFrameworkBadRequest()

        assertThat(entity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(entity.body).isEqualTo(
            WebErrorResponse(
                code = "bad_request",
                message = "입력값을 다시 확인한 뒤 시도해 주세요.",
            ),
        )
    }

    @Test
    fun `web page maps invalid application state to html error page`() {
        val modelAndView = pageHandler.handleApplicationException(
            ApplicationInvalidStateException("invalid projection"),
        )

        assertThat(modelAndView.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(modelAndView.viewName).isEqualTo("error/application-error")
        assertThat(modelAndView.model).containsEntry("statusCode", 500)
        assertThat(modelAndView.model).containsEntry("errorTitle", "일시적인 문제가 발생했습니다")
        assertThat(modelAndView.model).containsEntry("errorMessage", "잠시 뒤 다시 시도해 주세요.")
    }
}
