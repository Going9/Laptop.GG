package going9.laptopgg.job.crawler.client

import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DanawaHttpClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val requestPacingLock = Any()

    @Volatile
    private var nextAllowedRequestAtMillis = 0L

    @Volatile
    private var globalCooldownUntilMillis = 0L

    internal fun send(request: HttpRequest): String {
        var lastException: Exception? = null

        repeat(MAX_HTTP_RETRIES) { attempt ->
            try {
                awaitRequestSlot()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                if (response.statusCode() in 200..299) {
                    return response.body()
                }

                if (response.statusCode() in RETRYABLE_STATUS_CODES && attempt < MAX_HTTP_RETRIES - 1) {
                    val cooldown = retryDelayMillis(attempt, response.statusCode())
                    extendGlobalCooldown(cooldown)
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

                if (attempt == MAX_HTTP_RETRIES - 1) {
                    throw e
                }

                val cooldown = retryDelayMillis(attempt)
                extendGlobalCooldown(cooldown)
                logger.warn("I/O 오류로 요청을 재시도합니다. wait={}ms, uri={}, reason={}", cooldown, request.uri(), e.message)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("HTTP 요청이 중단되었습니다: ${request.uri()}", e)
            }
        }

        throw IllegalStateException("HTTP 요청 실패: ${request.uri()}", lastException)
    }

    private fun awaitRequestSlot() {
        while (true) {
            val waitMillis = synchronized(requestPacingLock) {
                val now = System.currentTimeMillis()
                val allowedAt = maxOf(now, nextAllowedRequestAtMillis, globalCooldownUntilMillis)
                if (allowedAt <= now) {
                    nextAllowedRequestAtMillis = now + MIN_REQUEST_INTERVAL_MILLIS + randomJitterMillis(REQUEST_JITTER_MILLIS)
                    0L
                } else {
                    allowedAt - now
                }
            }

            if (waitMillis <= 0L) {
                return
            }

            Thread.sleep(waitMillis)
        }
    }

    private fun extendGlobalCooldown(delayMillis: Long) {
        if (delayMillis <= 0L) {
            return
        }

        synchronized(requestPacingLock) {
            val candidate = System.currentTimeMillis() + delayMillis
            if (candidate > globalCooldownUntilMillis) {
                globalCooldownUntilMillis = candidate
            }
        }
    }

    private fun retryDelayMillis(attempt: Int, statusCode: Int? = null): Long {
        val baseDelay = when (statusCode) {
            429 -> 4_000L
            403 -> 2_500L
            500, 502, 503, 504 -> 1_500L
            else -> RETRY_DELAY_MILLIS
        }
        val exponential = baseDelay * (1L shl attempt.coerceAtMost(4))
        return minOf(MAX_RETRY_DELAY_MILLIS, exponential) + randomJitterMillis(RETRY_JITTER_MILLIS)
    }

    private fun randomJitterMillis(maxJitterMillis: Long): Long {
        if (maxJitterMillis <= 0L) {
            return 0L
        }

        return ThreadLocalRandom.current().nextLong(maxJitterMillis + 1)
    }

    private companion object {
        const val MAX_HTTP_RETRIES = 3
        const val RETRY_DELAY_MILLIS = 800L
        const val MAX_RETRY_DELAY_MILLIS = 8_000L
        const val RETRY_JITTER_MILLIS = 400L
        const val MIN_REQUEST_INTERVAL_MILLIS = 120L
        const val REQUEST_JITTER_MILLIS = 80L
        val RETRYABLE_STATUS_CODES = setOf(403, 429, 500, 502, 503, 504)
    }
}
