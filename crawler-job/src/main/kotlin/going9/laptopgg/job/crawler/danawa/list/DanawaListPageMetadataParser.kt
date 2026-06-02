package going9.laptopgg.job.crawler.danawa.list

import going9.laptopgg.job.crawler.danawa.DanawaEndpoints
import going9.laptopgg.job.crawler.list.ListPageMetadata
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal object DanawaListPageMetadataParser {
    fun parse(html: String, currentPage: Int): ListPageMetadata {
        val document = Jsoup.parse(html, DanawaEndpoints.NOTEBOOK_LIST_URL)
        return ListPageMetadata(
            hasNextPage = hasNextPage(document.selectFirst(".num_nav_wrap")?.let(::PageNavigation), currentPage),
            priceCompareCount = extractPriceCompareCount(document),
            visiblePageNumbers = extractVisiblePageNumbers(document),
            nextPageHint = extractNextPageHint(document),
        )
    }

    private fun hasNextPage(navigation: PageNavigation?, currentPage: Int): Boolean {
        navigation ?: return false

        val hasHigherNumberedPage = navigation.visiblePageNumbers.any { page -> page > currentPage }
        if (hasHigherNumberedPage) {
            return true
        }

        return navigation.hasNextLink
    }

    private fun extractPriceCompareCount(document: Document): Int? {
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

    private fun extractVisiblePageNumbers(document: Document): List<Int> {
        return document.select(".num_nav_wrap .number_wrap a.num")
            .mapNotNull { it.text().trim().toIntOrNull() }
    }

    private fun extractNextPageHint(document: Document): Int? {
        val onclick = document.selectFirst(".num_nav_wrap .edge_nav.nav_next")
            ?.attr("onclick")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return MOVE_PAGE_REGEX.find(onclick)?.groupValues?.get(1)?.toIntOrNull()
    }

    private class PageNavigation(
        private val element: Element,
    ) {
        val visiblePageNumbers: List<Int>
            get() = element.select(".number_wrap a.num")
                .mapNotNull { anchor -> anchor.text().trim().toIntOrNull() }

        val hasNextLink: Boolean
            get() = element.selectFirst(".edge_nav.nav_next") != null
    }

    private val PRICE_COMPARE_COUNT_REGEX = Regex("""\(([\d,]+)\)""")
    private val MOVE_PAGE_REGEX = Regex("""movePage\((\d+)\)""")
}
