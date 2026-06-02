package going9.laptopgg.job.crawler.danawa.client

import org.springframework.stereotype.Component

@Component
internal class DanawaRequestPacer(
    private val clock: DanawaClock = SystemDanawaClock(),
    private val sleeper: DanawaSleeper = ThreadDanawaSleeper(),
    private val jitterSource: DanawaJitterSource = ThreadLocalDanawaJitterSource(),
) {
    private val requestPacingLock = Any()

    @Volatile
    private var nextAllowedRequestAtMillis = 0L

    @Volatile
    private var globalCooldownUntilMillis = 0L

    internal fun awaitRequestSlot() {
        while (true) {
            val waitMillis = synchronized(requestPacingLock) {
                val now = clock.currentTimeMillis()
                val allowedAt = maxOf(now, nextAllowedRequestAtMillis, globalCooldownUntilMillis)
                if (allowedAt <= now) {
                    nextAllowedRequestAtMillis = now + MIN_REQUEST_INTERVAL_MILLIS + jitterSource.nextLong(REQUEST_JITTER_MILLIS)
                    0L
                } else {
                    allowedAt - now
                }
            }

            if (waitMillis <= 0L) {
                return
            }

            sleeper.sleep(waitMillis)
        }
    }

    internal fun extendGlobalCooldown(delayMillis: Long) {
        if (delayMillis <= 0L) {
            return
        }

        synchronized(requestPacingLock) {
            val candidate = clock.currentTimeMillis() + delayMillis
            if (candidate > globalCooldownUntilMillis) {
                globalCooldownUntilMillis = candidate
            }
        }
    }

    private companion object {
        const val MIN_REQUEST_INTERVAL_MILLIS = 120L
        const val REQUEST_JITTER_MILLIS = 80L
    }
}
