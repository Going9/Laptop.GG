package going9.laptopgg.job.crawler.detail

import org.jsoup.Jsoup

internal object DanawaSummaryFallbackParser {
    fun extractSummaryText(detailPageHtml: String): String {
        return Jsoup.parse(detailPageHtml, DANAWA_ORIGIN)
            .selectFirst(".summary_info .spec_list")
            ?.text()
            .orEmpty()
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
            )?.let(DanawaSpecValueParser::normalizeCpuManufacturer),
            cpu = extractFirst(normalizedText, Regex("""\[CPU\][^\[]*?/\s*([A-Za-z0-9\-]+)\s*\(""", RegexOption.IGNORE_CASE)),
            os = extractFirst(
                normalizedText,
                Regex("""(OS미포함\(프리도스\)|윈도우11홈|윈도우11프로|윈도우11|윈도우10 프로|윈도우10|macOS|리눅스|크롬OS|Whale OS)"""),
            ),
            screenSize = DanawaSpecValueParser.parseScreenSize(extractFirst(normalizedText, Regex("""([0-9.]+cm\([0-9.]+인치\))"""))),
            resolution = extractFirst(normalizedText, Regex("""해상도\s*:\s*([0-9]+x[0-9]+\([^)]+\))""")),
            brightness = DanawaSpecValueParser.parseIntValue(extractFirst(normalizedText, Regex("""밝기\s*:\s*([0-9]+nit)"""))),
            refreshRate = DanawaSpecValueParser.parseIntValue(extractFirst(normalizedText, Regex("""주사율\s*:\s*([0-9]+Hz)"""))),
            ramSize = DanawaSpecValueParser.parseCapacityInGb(extractFirst(normalizedText, Regex("""램\s*:\s*([0-9]+GB)"""))),
            isRamReplaceable = DanawaSpecValueParser.parsePossible(extractFirst(normalizedText, Regex("""램 교체\s*:\s*(가능|불가능)"""))),
            graphicsKind = extractFirst(normalizedText, Regex("""\[그래픽\]\s*([^/]+)"""))?.trim(),
            graphicsModel = extractFirst(normalizedText, Regex("""\[그래픽\]\s*[^/]+/\s*([^/]+)"""))?.trim(),
            tgp = DanawaSpecValueParser.parseIntValue(extractFirst(normalizedText, Regex("""TGP\s*:\s*([0-9]+W)"""))),
            isSupportsPdCharging = normalizedText.contains("USB-PD"),
            batteryCapacity = DanawaSpecValueParser.parseDoubleValue(extractFirst(normalizedText, Regex("""배터리\s*:\s*([0-9.]+Wh)"""))),
            storageCapacity = DanawaSpecValueParser.parseCapacityInGb(extractFirst(normalizedText, Regex("""용량\s*:\s*([0-9.]+(?:TB|GB))"""))),
            storageSlotCount = DanawaSpecValueParser.parseCountValue(extractFirst(normalizedText, Regex("""저장 슬롯\s*:\s*([0-9]+개)"""))),
            weight = DanawaSpecValueParser.parseWeightValue(normalizedText),
            usages = extractFirst(normalizedText, Regex("""용도\s*:\s*([^\[]+)"""))
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
        )
    }

    private fun extractFirst(text: String, regex: Regex): String? {
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private const val DANAWA_ORIGIN = "https://prod.danawa.com"
}
