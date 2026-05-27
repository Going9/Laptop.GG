package going9.laptopgg.service.crawler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Jsoup
import kotlin.math.roundToInt

internal object DanawaDetailParser {
    private val objectMapper = jacksonObjectMapper()

    fun extractDetailRequestContext(detailPageHtml: String): DetailRequestContext? {
        val match = PRODUCT_DESCRIPTION_INFO_REGEX.find(detailPageHtml) ?: return null
        val infoMap = objectMapper.readValue(match.groupValues[1], Map::class.java)
            .mapNotNull { (key, value) ->
                val stringKey = key as? String ?: return@mapNotNull null
                val stringValue = value as? String ?: return@mapNotNull null
                stringKey to stringValue
            }
            .toMap()

        return DetailRequestContext(
            makerName = infoMap["makerName"]?.trim(),
            productName = infoMap["productName"]?.trim(),
            prodType = infoMap["prodType"]?.trim(),
        )
    }

    fun extractSummaryText(detailPageHtml: String): String {
        return Jsoup.parse(detailPageHtml, DANAWA_ORIGIN)
            .selectFirst(".summary_info .spec_list")
            ?.text()
            .orEmpty()
    }

    fun parseSpecTable(html: String): ParsedSpecTable {
        val document = Jsoup.parse(html, DANAWA_ORIGIN)
        val specTable = document.selectFirst("table.spec_tbl")
            ?: return ParsedSpecTable(emptyMap(), emptyList())

        val values = linkedMapOf<String, String>()
        val usages = mutableListOf<String>()
        var currentSection: String? = null

        specTable.select("tr").forEach { row ->
            val children = row.children()
            if (children.isEmpty()) {
                return@forEach
            }

            if (children.size == 1 && children.first()?.tagName() == "th" && row.select("td").isEmpty()) {
                currentSection = children.first()!!.text().trim()
                return@forEach
            }

            var index = 0
            while (index + 1 < children.size) {
                val keyCell = children[index]
                val valueCell = children.getOrNull(index + 1)

                if (keyCell.tagName() == "th" && valueCell?.tagName() == "td") {
                    val key = keyCell.text().trim()
                    val value = valueCell.text().trim()

                    if (key.isNotBlank()) {
                        if (currentSection == "용도" && value == "○") {
                            usages += key
                        } else if (value.isNotBlank()) {
                            values[key] = value
                        }
                    }
                }

                index += 2
            }
        }

        return ParsedSpecTable(
            values = values,
            usages = usages.distinct(),
        )
    }

    fun parseSummaryFallback(summaryText: String): SummaryFallback {
        val normalizedText = summaryText.replace(Regex("\\s+"), " ").trim()
        if (normalizedText.isBlank()) {
            return SummaryFallback()
        }

        return SummaryFallback(
            cpuManufacturer = extractFirst(
                normalizedText,
                Regex("""\[CPU\]\s*(인텔|Intel|AMD|APPLE|Apple|퀄컴|Qualcomm)""", RegexOption.IGNORE_CASE),
            )?.let(::normalizeCpuManufacturer),
            cpu = extractFirst(normalizedText, Regex("""\[CPU\][^\[]*?/\s*([A-Za-z0-9\-]+)\s*\(""", RegexOption.IGNORE_CASE)),
            os = extractFirst(
                normalizedText,
                Regex("""(OS미포함\(프리도스\)|윈도우11홈|윈도우11프로|윈도우11|윈도우10 프로|윈도우10|macOS|리눅스|크롬OS|Whale OS)"""),
            ),
            screenSize = parseScreenSize(extractFirst(normalizedText, Regex("""([0-9.]+cm\([0-9.]+인치\))"""))),
            resolution = extractFirst(normalizedText, Regex("""해상도\s*:\s*([0-9]+x[0-9]+\([^)]+\))""")),
            brightness = parseIntValue(extractFirst(normalizedText, Regex("""밝기\s*:\s*([0-9]+nit)"""))),
            refreshRate = parseIntValue(extractFirst(normalizedText, Regex("""주사율\s*:\s*([0-9]+Hz)"""))),
            ramSize = parseCapacityInGb(extractFirst(normalizedText, Regex("""램\s*:\s*([0-9]+GB)"""))),
            isRamReplaceable = parsePossible(extractFirst(normalizedText, Regex("""램 교체\s*:\s*(가능|불가능)"""))),
            graphicsKind = extractFirst(normalizedText, Regex("""\[그래픽\]\s*([^/]+)"""))?.trim(),
            graphicsModel = extractFirst(normalizedText, Regex("""\[그래픽\]\s*[^/]+/\s*([^/]+)"""))?.trim(),
            tgp = parseIntValue(extractFirst(normalizedText, Regex("""TGP\s*:\s*([0-9]+W)"""))),
            isSupportsPdCharging = normalizedText.contains("USB-PD"),
            batteryCapacity = parseDoubleValue(extractFirst(normalizedText, Regex("""배터리\s*:\s*([0-9.]+Wh)"""))),
            storageCapacity = parseCapacityInGb(extractFirst(normalizedText, Regex("""용량\s*:\s*([0-9.]+(?:TB|GB))"""))),
            storageSlotCount = parseCountValue(extractFirst(normalizedText, Regex("""저장 슬롯\s*:\s*([0-9]+개)"""))),
            weight = parseWeightValue(normalizedText),
            usages = extractFirst(normalizedText, Regex("""용도\s*:\s*([^\[]+)"""))
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
        )
    }

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
        return when {
            rawManufacturer.contains("intel", ignoreCase = true) || rawManufacturer.contains("인텔") -> "인텔"
            rawManufacturer.contains("amd", ignoreCase = true) -> "AMD"
            rawManufacturer.contains("apple", ignoreCase = true) || rawManufacturer.contains("애플") -> "애플(ARM)"
            rawManufacturer.contains("qualcomm", ignoreCase = true) || rawManufacturer.contains("퀄컴") -> "퀄컴"
            else -> rawManufacturer.trim()
        }
    }

    fun isEmpty(summaryFallback: SummaryFallback): Boolean {
        return summaryFallback.cpuManufacturer == null &&
            summaryFallback.cpu == null &&
            summaryFallback.os == null &&
            summaryFallback.screenSize == null &&
            summaryFallback.resolution == null &&
            summaryFallback.brightness == null &&
            summaryFallback.refreshRate == null &&
            summaryFallback.ramSize == null &&
            summaryFallback.ramType == null &&
            summaryFallback.isRamReplaceable == null &&
            summaryFallback.graphicsKind == null &&
            summaryFallback.graphicsModel == null &&
            summaryFallback.tgp == null &&
            summaryFallback.isSupportsPdCharging == null &&
            summaryFallback.batteryCapacity == null &&
            summaryFallback.storageCapacity == null &&
            summaryFallback.storageSlotCount == null &&
            summaryFallback.weight == null &&
            summaryFallback.usages.isEmpty()
    }

    private fun extractFirst(text: String, regex: Regex): String? {
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private const val DANAWA_ORIGIN = "https://prod.danawa.com"
    private val PRODUCT_DESCRIPTION_INFO_REGEX = Regex(
        """var\s+oProductDescriptionInfo\s*=\s*(\{.*?});""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
}
