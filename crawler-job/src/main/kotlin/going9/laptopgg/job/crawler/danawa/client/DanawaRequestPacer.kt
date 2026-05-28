package going9.laptopgg.job.crawler.danawa.client

import java.util.concurrent.ThreadLocalRandom
import org.springframework.stereotype.Component

@Component
class DanawaRequestPacer {
    private val requestPacingLock = Any()

    @Volatile
    private var nextAllowedRequestAtMillis = 0L

    @Volatile
    private var globalCooldownUntilMillis = 0L

    internal fun awaitRequestSlot() {
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

    internal fun extendGlobalCooldown(delayMillis: Long) {
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

    private fun randomJitterMillis(maxJitterMillis: Long): Long {
        if (maxJitterMillis <= 0L) {
            return 0L
        }

        return ThreadLocalRandom.current().nextLong(maxJitterMillis + 1)
    }

    private companion object {
        const val MIN_REQUEST_INTERVAL_MILLIS = 120L
        const val REQUEST_JITTER_MILLIS = 80L
    }
}
