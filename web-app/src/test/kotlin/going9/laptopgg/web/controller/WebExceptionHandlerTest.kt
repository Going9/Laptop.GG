package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationInvalidStateException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest

class WebExceptionHandlerTest {
    private val handler = WebExceptionHandler()

    @Test
    fun `web api maps invalid application state to 500 response`() {
        val response = handler.handleApplicationException(
            ApplicationInvalidStateException("invalid projection"),
            MockHttpServletRequest("GET", "/api/recommends"),
        )

        assertThat(response).isInstanceOf(ResponseEntity::class.java)
        val entity = response as ResponseEntity<*>
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
        val response = handler.handleFrameworkBadRequest(
            MockHttpServletRequest("POST", "/api/recommends"),
        )

        assertThat(response).isInstanceOf(ResponseEntity::class.java)
        val entity = response as ResponseEntity<*>
        assertThat(entity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(entity.body).isEqualTo(
            WebErrorResponse(
                code = "bad_request",
                message = "입력값을 다시 확인한 뒤 시도해 주세요.",
            ),
        )
    }
}
