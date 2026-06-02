package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.PortabilityTier

internal class MobilityMetricPolicy {
    fun portabilityScore(weight: Double?): Int {
        val value = weight ?: return 40
        return when {
            value <= 0.9 -> 100
            value <= 1.1 -> 95
            value <= 1.3 -> 90
            value <= 1.5 -> 82
            value <= 1.7 -> 74
            value <= 2.0 -> 60
            value <= 2.3 -> 45
            value <= 2.6 -> 30
            else -> 15
        }
    }

    fun portabilityTier(weight: Double?): PortabilityTier {
        val value = weight ?: return PortabilityTier.UNKNOWN
        return when {
            value <= 1.0 -> PortabilityTier.TABLET_LIGHT
            value <= 1.3 -> PortabilityTier.ULTRALIGHT
            value <= 1.6 -> PortabilityTier.LIGHT
            value <= 2.2 -> PortabilityTier.BALANCED
            else -> PortabilityTier.HEAVY
        }
    }
}
