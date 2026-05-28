package going9.laptopgg.job.crawler.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DanawaRetryPolicyTest {
    private val retryPolicy = DanawaRetryPolicy()

    @Test
    fun `retries only retryable statuses before last attempt`() {
        assertThat(retryPolicy.shouldRetry(statusCode = 429, attempt = 0)).isTrue()
        assertThat(retryPolicy.shouldRetry(statusCode = 503, attempt = 1)).isTrue()
        assertThat(retryPolicy.shouldRetry(statusCode = 404, attempt = 0)).isFalse()
        assertThat(retryPolicy.shouldRetry(statusCode = 429, attempt = retryPolicy.maxAttempts - 1)).isFalse()
    }

    @Test
    fun `uses status-specific bounded retry delay`() {
        assertThat(retryPolicy.retryDelayMillis(attempt = 0, statusCode = 429)).isBetween(4_000L, 4_400L)
        assertThat(retryPolicy.retryDelayMillis(attempt = 0, statusCode = 403)).isBetween(2_500L, 2_900L)
        assertThat(retryPolicy.retryDelayMillis(attempt = 0, statusCode = 503)).isBetween(1_500L, 1_900L)
        assertThat(retryPolicy.retryDelayMillis(attempt = 10, statusCode = 429)).isBetween(8_000L, 8_400L)
    }
}
