package going9.laptopgg.job.crawler.danawa.client

import java.io.IOException
import java.net.http.HttpRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class DanawaHttpClient(
    private val requestPacer: DanawaRequestPacer,
    private val retryPolicy: DanawaRetryPolicy,
    private val transport: DanawaHttpTransport,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun send(request: HttpRequest): String {
        var lastException: Exception? = null

        repeat(retryPolicy.maxAttempts) { attempt ->
            try {
                requestPacer.awaitRequestSlot()
                val response = transport.send(request)
                if (response.statusCode() in 200..299) {
                    return response.body()
                }

                if (retryPolicy.shouldRetry(response.statusCode(), attempt)) {
                    val cooldown = retryPolicy.retryDelayMillis(attempt, response.statusCode())
                    requestPacer.extendGlobalCooldown(cooldown)
                    logger.warn(
                        "재시도 가능한 HTTP 상태를 감지해 잠시 대기합니다. status={}, wait={}ms, uri={}",
                        response.statusCode(),
                        cooldown,
                        request.uri(),
                    )
                    return@repeat
                }

                throw DanawaHttpStatusException(response.statusCode(), request.uri())
            } catch (e: IOException) {
                lastException = e

                if (attempt == retryPolicy.maxAttempts - 1) {
                    throw DanawaHttpRequestException(request.uri(), e)
                }

                val cooldown = retryPolicy.retryDelayMillis(attempt)
                requestPacer.extendGlobalCooldown(cooldown)
                logger.warn("I/O 오류로 요청을 재시도합니다. wait={}ms, uri={}, reason={}", cooldown, request.uri(), e.message)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw DanawaHttpInterruptedException(request.uri(), e)
            }
        }

        throw DanawaHttpRequestException(request.uri(), lastException)
    }
}
