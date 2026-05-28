package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationException
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
internal class WebExceptionHandler {
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(exception: ApplicationException): ResponseEntity<WebErrorResponse> {
        val (status, code) = when (exception) {
            is ResourceNotFoundException -> HttpStatus.NOT_FOUND to "not_found"
            is InvalidCommandException -> HttpStatus.BAD_REQUEST to "bad_request"
            is AuthenticationFailedException -> HttpStatus.FORBIDDEN to "forbidden"
        }

        return ResponseEntity
            .status(status)
            .body(WebErrorResponse(code = code, message = exception.message ?: code))
    }
}

internal data class WebErrorResponse(
    val code: String,
    val message: String,
)
