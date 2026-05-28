package going9.laptopgg.job.crawler.list

import going9.laptopgg.job.crawler.source.CrawlSource
import org.jsoup.Jsoup

internal object DanawaListParser {
    fun parseListPage(html: String): List<ProductCard> {
        val document = Jsoup.parse(html, NOTEBOOK_LIST_URL)

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

    fun hasNextPage(html: String, currentPage: Int): Boolean {
        val document = Jsoup.parse(html, NOTEBOOK_LIST_URL)
        val navigation = document.selectFirst(".num_nav_wrap") ?: return false

        val hasHigherNumberedPage = navigation.select(".number_wrap a.num")
            .mapNotNull { anchor -> anchor.text().trim().toIntOrNull() }
            .any { page -> page > currentPage }

        if (hasHigherNumberedPage) {
            return true
        }

        return navigation.selectFirst(".edge_nav.nav_next") != null
    }

    fun extractPriceCompareCount(html: String): Int? {
        val document = Jsoup.parse(html, NOTEBOOK_LIST_URL)
        val hiddenCount = document.selectFirst("#totalProductCount")
            ?.attr("value")
            ?.takeIf { it.isNotBlank() }
            ?.replace(",", "")
            ?.toIntOrNull()
        if (hiddenCount != null) {
            return hiddenCount
        }

        return document.select(".tab_list_nav a")
            .map { it.text().trim() }
            .firstOrNull { it.contains("가격비교") }
            ?.let { PRICE_COMPARE_COUNT_REGEX.find(it)?.groupValues?.get(1) }
            ?.replace(",", "")
            ?.toIntOrNull()
    }

    fun extractVisiblePageNumbers(html: String): List<Int> {
        val document = Jsoup.parse(html, NOTEBOOK_LIST_URL)
        return document.select(".num_nav_wrap .number_wrap a.num")
            .mapNotNull { it.text().trim().toIntOrNull() }
    }

    fun extractNextPageHint(html: String): Int? {
        val document = Jsoup.parse(html, NOTEBOOK_LIST_URL)
        val onclick = document.selectFirst(".num_nav_wrap .edge_nav.nav_next")
            ?.attr("onclick")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return MOVE_PAGE_REGEX.find(onclick)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun createPageSignature(productCards: List<ProductCard>): String {
        return productCards.joinToString("||") { it.detailPage }
    }

    fun extractListRequestContext(
        initialListHtml: String,
        crawlSource: CrawlSource,
    ): ListRequestContext {
        val defaults = ListRequestContext()

        return ListRequestContext(
            listUrl = crawlSource.listUrl,
            listCategoryCode = extractJsScalar(initialListHtml, "nListCategoryCode") ?: defaults.listCategoryCode,
            categoryCode = extractJsScalar(initialListHtml, "nCategoryCode") ?: defaults.categoryCode,
            physicsCate1 = extractJsScalar(initialListHtml, "sPhysicsCate1") ?: defaults.physicsCate1,
            physicsCate2 = extractJsScalar(initialListHtml, "sPhysicsCate2") ?: defaults.physicsCate2,
            physicsCate3 = extractJsScalar(initialListHtml, "sPhysicsCate3") ?: defaults.physicsCate3,
            physicsCate4 = extractJsScalar(initialListHtml, "sPhysicsCate4") ?: defaults.physicsCate4,
            viewMethod = extractJsScalar(initialListHtml, "sPriceCompareListType") ?: defaults.viewMethod,
            sortMethod = extractJsScalar(initialListHtml, "sPriceCompareListSort")
                ?: extractJsScalar(initialListHtml, "sProductListSort")
                ?: defaults.sortMethod,
            listCount = extractJsScalar(initialListHtml, "nPriceCompareListCount") ?: defaults.listCount,
            group = extractJsScalar(initialListHtml, "nListGroup") ?: defaults.group,
            depth = extractJsScalar(initialListHtml, "nListDepth") ?: defaults.depth,
            discountProductRate = extractJsScalar(initialListHtml, "sDiscountProductRate") ?: defaults.discountProductRate,
            initialPriceDisplay = extractJsScalar(initialListHtml, "sInitialPriceDisplay") ?: defaults.initialPriceDisplay,
            quickDeliveryCategoryYn = extractJsScalar(initialListHtml, "quickDeliveryCategoryYN") ?: defaults.quickDeliveryCategoryYn,
            quickDeliveryDisplay = extractJsScalar(initialListHtml, "quickDeliveryDisplay") ?: defaults.quickDeliveryDisplay,
            priceUnitSort = extractJsScalar(initialListHtml, "priceUnitSort") ?: defaults.priceUnitSort,
            priceUnitSortOrder = extractJsScalar(initialListHtml, "priceUnitSortOrder") ?: defaults.priceUnitSortOrder,
            simpleDescriptionDisplayYn = extractJsScalar(initialListHtml, "simpleDescriptionDisplayYN") ?: defaults.simpleDescriptionDisplayYn,
            simpleDescriptionOpen = extractJsScalar(initialListHtml, "simpleDescriptionOpen") ?: defaults.simpleDescriptionOpen,
            listPackageType = extractJsScalar(initialListHtml, "nPriceCompareListPackageType") ?: defaults.listPackageType,
            priceUnit = extractJsScalar(initialListHtml, "nPriceUnit") ?: defaults.priceUnit,
            priceUnitValue = extractJsScalar(initialListHtml, "nPriceUnitValue") ?: defaults.priceUnitValue,
            priceUnitClass = extractJsScalar(initialListHtml, "sPriceUnitClass") ?: defaults.priceUnitClass,
            cmRecommendSort = extractJsScalar(initialListHtml, "sCmRecommendSort") ?: defaults.cmRecommendSort,
            cmRecommendSortDefault = extractJsScalar(initialListHtml, "sCmRecommendSortDefault") ?: defaults.cmRecommendSortDefault,
            bundleImagePreview = extractJsScalar(initialListHtml, "sBundleImagePreview") ?: defaults.bundleImagePreview,
            packageLimit = extractJsScalar(initialListHtml, "nPriceCompareListPackageLimit") ?: defaults.packageLimit,
            makerDisplayYn = extractJsScalar(initialListHtml, "sMakerStandardDisplayStatus")
                ?: extractJsScalar(initialListHtml, "sMakerIndicate")
                ?: defaults.makerDisplayYn,
            dpgZoneUiCategory = extractJsScalar(initialListHtml, "isDpgZoneUICategory") ?: defaults.dpgZoneUiCategory,
            assemblyGalleryCategory = extractJsScalar(initialListHtml, "isAssemblyGalleryCategory") ?: defaults.assemblyGalleryCategory,
            searchAttributeValues = crawlSource.attributeFilters.map { it.value },
        )
    }

    fun extractQueryParam(url: String, key: String): String? {
        return Regex("""(?:\?|&)$key=([^&#]+)""").find(url)?.groupValues?.getOrNull(1)
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
        return "$DANAWA_ORIGIN/info/?pcode=$productCode&cate=$cate"
    }

    private fun extractJsScalar(html: String, key: String): String? {
        val escapedKey = Regex.escape(key)

        Regex("""["']?$escapedKey["']?\s*:\s*"([^"]*)"""")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        return Regex("""["']?$escapedKey["']?\s*:\s*([0-9]+)""")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private const val NOTEBOOK_LIST_URL = "https://prod.danawa.com/list/?cate=112758"
    private const val DANAWA_ORIGIN = "https://prod.danawa.com"
    private val PRICE_COMPARE_COUNT_REGEX = Regex("""\(([\d,]+)\)""")
    private val MOVE_PAGE_REGEX = Regex("""movePage\((\d+)\)""")
}
