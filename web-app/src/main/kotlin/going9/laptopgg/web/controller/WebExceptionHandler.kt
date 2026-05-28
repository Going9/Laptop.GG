package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.ApplicationException
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.ModelAndView

@ControllerAdvice
internal class WebExceptionHandler {
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(
        exception: ApplicationException,
        request: HttpServletRequest,
    ): Any {
        val error = WebErrorDescriptor.from(exception)
        if (request.requestURI.startsWith("/api/")) {
            return ResponseEntity
                .status(error.status)
                .body(WebErrorResponse(code = error.code, message = error.message))
        }

        return ModelAndView(
            "error/application-error",
            mapOf(
                "statusCode" to error.status.value(),
                "errorTitle" to error.title,
                "errorMessage" to error.message,
            ),
            error.status,
        )
    }
}

private data class WebErrorDescriptor(
    val status: HttpStatus,
    val code: String,
    val title: String,
    val message: String,
) {
    companion object {
        fun from(exception: ApplicationException): WebErrorDescriptor {
            return when (exception) {
                is ResourceNotFoundException -> WebErrorDescriptor(
                    status = HttpStatus.NOT_FOUND,
                    code = "not_found",
                    title = "요청한 정보를 찾을 수 없습니다",
                    message = "대상이 삭제되었거나 주소가 변경되었을 수 있습니다.",
                )
                is InvalidCommandException -> WebErrorDescriptor(
                    status = HttpStatus.BAD_REQUEST,
                    code = "bad_request",
                    title = "요청 내용을 확인해 주세요",
                    message = "입력값을 다시 확인한 뒤 시도해 주세요.",
                )
                is AuthenticationFailedException -> WebErrorDescriptor(
                    status = HttpStatus.FORBIDDEN,
                    code = "forbidden",
                    title = "요청을 처리할 권한이 없습니다",
                    message = "비밀번호 또는 요청 권한을 확인해 주세요.",
                )
                is ApplicationInvalidStateException -> WebErrorDescriptor(
                    status = HttpStatus.INTERNAL_SERVER_ERROR,
                    code = "internal_error",
                    title = "일시적인 문제가 발생했습니다",
                    message = "잠시 뒤 다시 시도해 주세요.",
                )
            }
        }
    }
}

internal data class WebErrorResponse(
    val code: String,
    val message: String,
)
