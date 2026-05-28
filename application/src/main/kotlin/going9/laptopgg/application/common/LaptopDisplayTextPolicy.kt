package going9.laptopgg.application.common

internal object LaptopDisplayTextPolicy {
    fun manufacturerName(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "브랜드 확인 불가"
    }

    fun resolutionLabel(resolution: String?): String? {
        val raw = resolution?.trim().orEmpty()
        if (raw.isBlank()) {
            return null
        }

        val normalized = raw.uppercase()
        return when {
            normalized.contains("UHD") || normalized.contains("4K") -> "UHD"
            normalized.contains("QHD") || normalized.contains("WQHD") || normalized.contains("WQXGA") -> "QHD"
            normalized.contains("FHD") || normalized.contains("WUXGA") -> "FHD"
            normalized.contains("HD") -> "HD"
            else -> {
                val match = RESOLUTION_REGEX.find(normalized) ?: return raw
                val width = match.groupValues[1].toIntOrNull() ?: return raw
                when {
                    width >= 3840 -> "UHD"
                    width >= 2560 -> "QHD"
                    width >= 1920 -> "FHD"
                    width >= 1280 -> "HD"
                    else -> raw
                }
            }
        }
    }

    fun humanizeOs(rawOs: String?): String? {
        val trimmed = rawOs?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalized = trimmed.lowercase()

        return when {
            normalized == "freedos" || normalized == "free dos" || normalized == "dos" -> "프리도스"
            normalized.contains("windows") -> trimmed
                .replace(Regex("(?i)windows"), "윈도우")
                .replace(Regex("(?i)home"), "홈")
                .replace(Regex("(?i)pro"), "프로")
            normalized.contains("macos") || normalized.contains("mac os") ->
                trimmed.replace(Regex("(?i)mac\\s?os"), "macOS")
            normalized.contains("linux") -> trimmed.replace(Regex("(?i)linux"), "리눅스")
            else -> trimmed
        }
    }

    private val RESOLUTION_REGEX = Regex("""(\d{3,4})\s*[xX]\s*(\d{3,4})""")
}
