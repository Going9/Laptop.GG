package going9.laptopgg.job.crawler.danawa.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DanawaRequestPacerTest {
    @Test
    fun `awaitRequestSlot sleeps until the next paced request slot`() {
        val clock = MutableDanawaClock(currentTimeMillis = 1_000L)
        val sleeper = RecordingDanawaSleeper(clock)
        val pacer = DanawaRequestPacer(
            clock = clock,
            sleeper = sleeper,
            jitterSource = FixedDanawaJitterSource(jitterMillis = 0L),
        )

        pacer.awaitRequestSlot()
        pacer.awaitRequestSlot()

        assertThat(sleeper.sleptMillis).containsExactly(120L)
    }

    @Test
    fun `extendGlobalCooldown delays the next request without real sleeping`() {
        val clock = MutableDanawaClock(currentTimeMillis = 2_000L)
        val sleeper = RecordingDanawaSleeper(clock)
        val pacer = DanawaRequestPacer(
            clock = clock,
            sleeper = sleeper,
            jitterSource = FixedDanawaJitterSource(jitterMillis = 0L),
        )

        pacer.extendGlobalCooldown(500L)
        pacer.awaitRequestSlot()

        assertThat(sleeper.sleptMillis).containsExactly(500L)
    }

    private class MutableDanawaClock(
        var currentTimeMillis: Long,
    ) : DanawaClock {
        override fun currentTimeMillis(): Long {
            return currentTimeMillis
        }
    }

    private class RecordingDanawaSleeper(
        private val clock: MutableDanawaClock,
    ) : DanawaSleeper {
        val sleptMillis = mutableListOf<Long>()

        override fun sleep(waitMillis: Long) {
            sleptMillis += waitMillis
            clock.currentTimeMillis += waitMillis
        }
    }

    private class FixedDanawaJitterSource(
        private val jitterMillis: Long,
    ) : DanawaJitterSource {
        override fun nextLong(maxInclusive: Long): Long {
            return jitterMillis.coerceAtMost(maxInclusive)
        }
    }
}
