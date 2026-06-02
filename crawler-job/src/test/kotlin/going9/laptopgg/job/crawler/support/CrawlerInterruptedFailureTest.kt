package going9.laptopgg.job.crawler.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawlerInterruptedFailureTest {
    @Test
    fun `detects interrupted cause and restores interrupt flag`() {
        val failure = IllegalStateException("wrapped", InterruptedException("stop"))

        try {
            assertThat(failure.isCrawlerInterruptedFailure()).isTrue()
            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `does not treat regular failures as interruption`() {
        assertThat(IllegalStateException("regular").isCrawlerInterruptedFailure()).isFalse()
    }
}
