package going9.laptopgg.application.common

import java.time.Instant
import java.time.temporal.ChronoUnit

internal object LaptopPriceFreshnessPolicy {
    fun minimumCheckedAt(): Instant = Instant.now().minus(48, ChronoUnit.HOURS)
}
