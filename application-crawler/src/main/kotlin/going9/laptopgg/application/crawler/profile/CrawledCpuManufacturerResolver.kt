package going9.laptopgg.application.crawler.profile

class CrawledCpuManufacturerResolver {
    fun resolve(rawManufacturer: String?, productName: String, rawCpu: String?): String? {
        rawManufacturer?.let(::normalize)?.let { return it }

        val normalizedName = productName.uppercase()
        val normalizedCpu = rawCpu.orEmpty().uppercase()

        return when {
            normalizedName.contains("APPLE") || normalizedName.contains("맥북") -> "애플(ARM)"
            normalizedName.contains("SNAPDRAGON") ||
                normalizedName.contains("X ELITE") ||
                normalizedName.contains("X PLUS") ||
                normalizedCpu.startsWith("X1") ||
                normalizedCpu.startsWith("X2") -> "퀄컴"
            else -> null
        }
    }

    fun normalize(rawManufacturer: String): String? {
        val value = rawManufacturer.trim().takeIf { it.isNotBlank() } ?: return null
        return when {
            value.contains("intel", ignoreCase = true) || value.contains("인텔") -> "인텔"
            value.contains("amd", ignoreCase = true) -> "AMD"
            value.contains("apple", ignoreCase = true) || value.contains("애플") -> "애플(ARM)"
            value.contains("qualcomm", ignoreCase = true) || value.contains("퀄컴") -> "퀄컴"
            else -> value
        }
    }
}
