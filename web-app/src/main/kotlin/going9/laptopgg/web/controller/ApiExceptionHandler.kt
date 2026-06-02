package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice(annotations = [RestController::class])
internal class ApiExceptionHandler {
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(exception: ApplicationException): ResponseEntity<WebErrorResponse> {
        return render(WebErrorDescriptor.from(exception))
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        BindException::class,
    )
    fun handleFrameworkBadRequest(): ResponseEntity<WebErrorResponse> {
        return render(WebErrorDescriptor.badRequest())
    }

    private fun render(error: WebErrorDescriptor): ResponseEntity<WebErrorResponse> {
        return ResponseEntity
            .status(error.status)
            .body(WebErrorResponse(code = error.code, message = error.message))
    }
}
