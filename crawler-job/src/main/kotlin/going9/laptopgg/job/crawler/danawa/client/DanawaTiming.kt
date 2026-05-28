package going9.laptopgg.job.crawler.danawa.client

import java.util.concurrent.ThreadLocalRandom
import org.springframework.stereotype.Component

internal fun interface DanawaClock {
    fun currentTimeMillis(): Long
}

@Component
internal class SystemDanawaClock : DanawaClock {
    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}

internal fun interface DanawaSleeper {
    fun sleep(waitMillis: Long)
}

@Component
internal class ThreadDanawaSleeper : DanawaSleeper {
    override fun sleep(waitMillis: Long) {
        Thread.sleep(waitMillis)
    }
}

internal fun interface DanawaJitterSource {
    fun nextLong(maxInclusive: Long): Long
}

@Component
internal class ThreadLocalDanawaJitterSource : DanawaJitterSource {
    override fun nextLong(maxInclusive: Long): Long {
        if (maxInclusive <= 0L) {
            return 0L
        }

        return ThreadLocalRandom.current().nextLong(maxInclusive + 1)
    }
}
