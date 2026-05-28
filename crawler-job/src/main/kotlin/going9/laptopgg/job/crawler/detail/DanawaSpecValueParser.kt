package going9.laptopgg.job.crawler.detail

import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import kotlin.math.roundToInt

internal object DanawaSpecValueParser {
    private val cpuManufacturerResolver = CrawledCpuManufacturerResolver()

    fun parseScreenSize(value: String?): Int? {
        val inches = Regex("""([0-9.]+)인치""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            ?: return null
        return inches.toInt()
    }

    fun parseIntValue(value: String?): Int? {
        return Regex("""([0-9]+)""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun parseDoubleValue(value: String?): Double? {
        return Regex("""([0-9]+(?:\.[0-9]+)?)""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    fun parseWeightValue(value: String?): Double? {
        val text = value.orEmpty()
        val kilogramWeights = Regex("""([0-9]+(?:\.[0-9]+)?)\s*kg""", RegexOption.IGNORE_CASE)
            .findAll(text)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .toList()
        val gramWeights = Regex("""([0-9]+(?:\.[0-9]+)?)\s*g""", RegexOption.IGNORE_CASE)
            .findAll(text)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull()?.div(1000.0) }
            .toList()

        return (kilogramWeights + gramWeights)
            .filter { it > 0 }
            .maxOrNull()
    }

    fun parseCapacityInGb(value: String?): Int? {
        val match = Regex("""([0-9]+(?:\.[0-9]+)?)(TB|GB)""", RegexOption.IGNORE_CASE).find(value.orEmpty()) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].uppercase()

        return when (unit) {
            "TB" -> (amount * 1024).roundToInt()
            "GB" -> amount.roundToInt()
            else -> null
        }
    }

    fun parseCountValue(value: String?): Int? {
        return Regex("""([0-9]+)개""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun parsePossible(value: String?): Boolean? {
        return when (value?.trim()) {
            "가능" -> true
            "불가능" -> false
            else -> null
        }
    }

    fun parseThunderboltCount(spec: Map<String, String>): Int? {
        val count = spec.entries
            .filter { it.key.startsWith("썬더볼트") }
            .sumOf { parseCountValue(it.value) ?: 0 }

        return count.takeIf { it > 0 }
    }

    fun parseUsbCCount(spec: Map<String, String>): Int? {
        val directCount = parseCountValue(spec["USB-C"])
        if (directCount != null) {
            return directCount
        }

        val thunderboltCount = spec.entries
            .filter { it.key.startsWith("썬더볼트") && it.value.contains("USB-C겸용") }
            .sumOf { parseCountValue(it.value) ?: 0 }

        return thunderboltCount.takeIf { it > 0 }
    }

    fun parseSdCard(spec: Map<String, String>): String? {
        return when {
            spec["SD카드"] == "○" -> "SD카드"
            spec["MicroSD카드"] == "○" -> "MicroSD카드"
            else -> null
        }
    }

    fun normalizeOs(rawOs: String?): String? {
        val value = rawOs?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (value.contains("미포함")) "freedos" else value
    }

    fun normalizeCpuManufacturer(rawManufacturer: String): String {
        return requireNotNull(cpuManufacturerResolver.normalize(rawManufacturer)) {
            "rawManufacturer must not be blank."
        }
    }
}
