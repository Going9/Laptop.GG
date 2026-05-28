package going9.laptopgg.application.crawler.profile

import kotlin.math.roundToInt

internal class DisplayMetricPolicy {
    fun displayScore(laptop: LaptopProfileSource): Int {
        val pixelScore = pixelScore(laptop.resolution)
        val brightnessScore = brightnessScore(laptop.brightness)
        val refreshScore = refreshScore(laptop.refreshRate)

        return clampScore((pixelScore * 0.60) + (brightnessScore * 0.25) + (refreshScore * 0.15))
    }

    private fun pixelScore(resolution: String?): Double {
        val resolutionMatch = RESOLUTION_REGEX.find(resolution.orEmpty())
        if (resolutionMatch == null) {
            return 50.0
        }

        val width = resolutionMatch.groupValues[1].toInt()
        val height = resolutionMatch.groupValues[2].toInt()
        return ((width.toDouble() * height) / REFERENCE_PIXELS * 100.0).coerceIn(20.0, 100.0)
    }

    private fun brightnessScore(brightness: Int?): Double {
        return when (brightness) {
            null -> 55.0
            else -> (brightness.toDouble() / 500.0 * 100.0).coerceIn(35.0, 100.0)
        }
    }

    private fun refreshScore(refreshRate: Int?): Double {
        return when (refreshRate) {
            null -> 50.0
            else -> (refreshRate.toDouble() / 240.0 * 100.0).coerceIn(25.0, 100.0)
        }
    }

    private fun clampScore(value: Double): Int {
        return value.roundToInt().coerceIn(0, 100)
    }

    private companion object {
        private val RESOLUTION_REGEX = Regex("""([0-9]{3,4})x([0-9]{3,4})""", RegexOption.IGNORE_CASE)
        private const val REFERENCE_PIXELS = 2880.0 * 1800.0
    }
}
