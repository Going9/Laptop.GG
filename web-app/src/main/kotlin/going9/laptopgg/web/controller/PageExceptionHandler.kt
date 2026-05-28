package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.ModelAndView

@ControllerAdvice(
    assignableTypes = [
        LaptopPageController::class,
        RecommendationPageController::class,
        CommentPageController::class,
    ],
)
internal class PageExceptionHandler {
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(exception: ApplicationException): ModelAndView {
        return render(WebErrorDescriptor.from(exception))
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        BindException::class,
    )
    fun handleFrameworkBadRequest(): ModelAndView {
        return render(WebErrorDescriptor.badRequest())
    }

    private fun render(error: WebErrorDescriptor): ModelAndView {
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
