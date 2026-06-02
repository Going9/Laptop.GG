package going9.laptopgg.web.controller

import going9.laptopgg.application.common.ApplicationException
import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import org.springframework.http.HttpStatus

internal data class WebErrorDescriptor(
    val status: HttpStatus,
    val code: String,
    val title: String,
    val message: String,
) {
    companion object {
        fun badRequest(): WebErrorDescriptor {
            return WebErrorDescriptor(
                status = HttpStatus.BAD_REQUEST,
                code = "bad_request",
                title = "요청 내용을 확인해 주세요",
                message = "입력값을 다시 확인한 뒤 시도해 주세요.",
            )
        }

        fun from(exception: ApplicationException): WebErrorDescriptor {
            return when (exception) {
                is ResourceNotFoundException -> WebErrorDescriptor(
                    status = HttpStatus.NOT_FOUND,
                    code = "not_found",
                    title = "요청한 정보를 찾을 수 없습니다",
                    message = "대상이 삭제되었거나 주소가 변경되었을 수 있습니다.",
                )
                is InvalidCommandException -> badRequest()
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
