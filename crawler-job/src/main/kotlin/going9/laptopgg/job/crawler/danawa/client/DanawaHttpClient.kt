package going9.laptopgg.job.crawler.danawa.client

import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DanawaHttpClient(
    private val requestPacer: DanawaRequestPacer,
    private val retryPolicy: DanawaRetryPolicy,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    internal fun send(request: HttpRequest): String {
        var lastException: Exception? = null

        repeat(retryPolicy.maxAttempts) { attempt ->
            try {
                requestPacer.awaitRequestSlot()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
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

                throw IllegalStateException("HTTP ${response.statusCode()} 요청 실패: ${request.uri()}")
            } catch (e: IOException) {
                lastException = e

                if (attempt == retryPolicy.maxAttempts - 1) {
                    throw e
                }

                val cooldown = retryPolicy.retryDelayMillis(attempt)
                requestPacer.extendGlobalCooldown(cooldown)
                logger.warn("I/O 오류로 요청을 재시도합니다. wait={}ms, uri={}, reason={}", cooldown, request.uri(), e.message)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("HTTP 요청이 중단되었습니다: ${request.uri()}", e)
            }
        }

        throw IllegalStateException("HTTP 요청 실패: ${request.uri()}", lastException)
    }
}
