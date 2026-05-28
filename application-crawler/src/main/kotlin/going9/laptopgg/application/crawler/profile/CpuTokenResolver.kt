package going9.laptopgg.application.crawler.profile

class CpuTokenResolver {
    fun resolve(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        rawCpu?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val normalizedName = normalizeKoreanAppleSuffix(productName.uppercase())

        if (isAppleCpuCandidate(cpuManufacturer, normalizedName)) {
            val appleCpu = findAppleCpuToken(normalizedName)
            if (appleCpu != null) {
                return listOf("M${appleCpu.generation}", appleCpu.suffix)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim()
            }
        }

        return when {
            normalizedName.contains("X ELITE") -> "X Elite"
            normalizedName.contains("X PLUS") -> "X Plus"
            else -> null
        }
    }

    fun normalize(cpu: String?): String {
        return normalizeKoreanAppleSuffix(cpu.orEmpty().uppercase())
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    internal fun findAppleCpuToken(normalizedCpu: String): AppleCpuToken? {
        val match = APPLE_CPU_REGEX.find(normalizedCpu) ?: return null
        return AppleCpuToken(
            generation = match.groupValues[1].toIntOrNull() ?: 1,
            suffix = match.groupValues[2].trim(),
        )
    }

    private fun isAppleCpuCandidate(cpuManufacturer: String?, normalizedName: String): Boolean {
        return cpuManufacturer.orEmpty().contains("애플") ||
            cpuManufacturer.orEmpty().contains("APPLE", ignoreCase = true) ||
            normalizedName.contains("MACBOOK") ||
            normalizedName.contains("맥북")
    }

    private fun normalizeKoreanAppleSuffix(value: String): String {
        return value
            .replace("프로", " PRO")
            .replace("맥스", " MAX")
            .replace("울트라", " ULTRA")
    }

    private companion object {
        private val APPLE_CPU_REGEX = Regex("""M([1-9])(?:\s*(PRO|MAX|ULTRA))?""", RegexOption.IGNORE_CASE)
    }
}

internal data class AppleCpuToken(
    val generation: Int,
    val suffix: String,
)
