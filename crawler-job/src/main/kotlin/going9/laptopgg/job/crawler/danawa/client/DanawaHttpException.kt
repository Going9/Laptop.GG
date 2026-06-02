package going9.laptopgg.job.crawler.danawa.client

import java.net.URI

internal sealed class DanawaHttpException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal class DanawaHttpStatusException(
    val statusCode: Int,
    val uri: URI,
) : DanawaHttpException("HTTP $statusCode 요청 실패: $uri")

internal class DanawaHttpRequestException(
    val uri: URI,
    cause: Throwable?,
) : DanawaHttpException("HTTP 요청 실패: $uri", cause)

internal class DanawaHttpInterruptedException(
    val uri: URI,
    cause: InterruptedException,
) : DanawaHttpException("HTTP 요청이 중단되었습니다: $uri", cause)
