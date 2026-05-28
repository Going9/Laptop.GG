package going9.laptopgg.job.crawler.list

import going9.laptopgg.job.crawler.danawa.DanawaEndpoints
import org.jsoup.Jsoup

internal object DanawaProductCardParser {
    fun parse(html: String): List<ProductCard> {
        val document = Jsoup.parse(html, DanawaEndpoints.NOTEBOOK_LIST_URL)

        return document.select("li.prod_item")
            .mapNotNull { productItem ->
                val productLink = productItem.selectFirst("a[name=productName]") ?: return@mapNotNull null
                val detailPage = productLink.attr("href").trim()
                val productCode = extractQueryParam(detailPage, "pcode") ?: return@mapNotNull null
                val cateValues = productItem.selectFirst(".prod_pricelist")
                    ?.attr("data-cate")
                    ?.split("|")
                    ?.map { it.trim() }
                    ?.takeIf { it.size == 4 }
                    ?: return@mapNotNull null

                val imageElement = productItem.selectFirst(".thumb_image img")
                val imageUrl = normalizeImageUrl(
                    imageElement?.attr("src")
                        ?.takeIf { it.isNotBlank() && !it.contains("noImg") }
                        ?: imageElement?.attr("data-original").orEmpty(),
                )

                val priceText = productItem.selectFirst(".prod_pricelist .text__number")?.text()
                    ?: productItem.selectFirst(".price_sect a")?.text()

                ProductCard(
                    productCode = productCode,
                    productName = productLink.text().trim(),
                    detailPage = normalizeDetailPage(detailPage, productCode, cateValues[3]),
                    imageUrl = imageUrl,
                    price = parsePrice(priceText),
                    cate1 = cateValues[0],
                    cate2 = cateValues[1],
                    cate3 = cateValues[2],
                    cate4 = cateValues[3],
                )
            }
            .distinctBy { it.detailPage }
    }

    private fun normalizeImageUrl(url: String): String {
        if (url.isBlank()) {
            return ""
        }

        val normalizedUrl = when {
            url.startsWith("//") -> "https:$url"
            else -> url
        }

        return normalizedUrl.replace(Regex("shrink=\\d+:\\d+"), "shrink=500:500")
    }

    private fun parsePrice(priceText: String?): Int? {
        return priceText
            ?.replace(Regex("[^0-9]"), "")
            ?.takeIf { it.isNotBlank() }
            ?.toIntOrNull()
    }

    private fun normalizeDetailPage(rawDetailPage: String, productCode: String, categoryCode: String): String {
        val cate = extractQueryParam(rawDetailPage, "cate") ?: categoryCode
        return DanawaEndpoints.productDetailUrl(productCode, cate)
    }

    private fun extractQueryParam(url: String, key: String): String? {
        return Regex("""(?:\?|&)$key=([^&#]+)""").find(url)?.groupValues?.getOrNull(1)
    }

}
