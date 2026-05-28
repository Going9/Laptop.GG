package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.PortabilityTier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MobilityAndBatteryMetricPolicyTest {
    private val mobilityMetricPolicy = MobilityMetricPolicy()
    private val batteryMetricPolicy = BatteryMetricPolicy()

    @Test
    fun `mobility policy maps weight to score and tier`() {
        assertThat(mobilityMetricPolicy.portabilityScore(1.2)).isEqualTo(90)
        assertThat(mobilityMetricPolicy.portabilityTier(1.2)).isEqualTo(PortabilityTier.ULTRALIGHT)
        assertThat(mobilityMetricPolicy.portabilityScore(null)).isEqualTo(40)
        assertThat(mobilityMetricPolicy.portabilityTier(null)).isEqualTo(PortabilityTier.UNKNOWN)
    }

    @Test
    fun `battery policy maps capacity to score and tier`() {
        assertThat(batteryMetricPolicy.batteryCapacityScore(80.0)).isEqualTo(92)
        assertThat(batteryMetricPolicy.batteryTier(80.0)).isEqualTo(BatteryTier.HIGH)
        assertThat(batteryMetricPolicy.batteryCapacityScore(null)).isEqualTo(35)
        assertThat(batteryMetricPolicy.batteryTier(null)).isEqualTo(BatteryTier.UNKNOWN)
    }
}
