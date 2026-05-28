package going9.laptopgg.job.crawler.danawa.client

import java.util.concurrent.ThreadLocalRandom
import org.springframework.stereotype.Component

@Component
internal class DanawaRetryPolicy {
    internal val maxAttempts: Int = 3

    internal fun shouldRetry(statusCode: Int, attempt: Int): Boolean {
        return statusCode in RETRYABLE_STATUS_CODES && attempt < maxAttempts - 1
    }

    internal fun retryDelayMillis(attempt: Int, statusCode: Int? = null): Long {
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
        const val RETRY_DELAY_MILLIS = 800L
        const val MAX_RETRY_DELAY_MILLIS = 8_000L
        const val RETRY_JITTER_MILLIS = 400L
        val RETRYABLE_STATUS_CODES = setOf(403, 429, 500, 502, 503, 504)
    }
}
