package going9.laptopgg.job.crawler.detail

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Jsoup

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

    private const val DANAWA_ORIGIN = "https://prod.danawa.com"
    private val PRODUCT_DESCRIPTION_INFO_REGEX = Regex(
        """var\s+oProductDescriptionInfo\s*=\s*(\{.*?});""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
}
