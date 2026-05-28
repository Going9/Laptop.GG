package going9.laptopgg.job.crawler.client

import going9.laptopgg.job.crawler.detail.DetailRequestContext
import going9.laptopgg.job.crawler.list.ListRequestContext
import going9.laptopgg.job.crawler.list.ProductCard
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.springframework.stereotype.Component

@Component
class DanawaRequestFactory {
    internal fun initialListPage(listUrl: String): HttpRequest {
        return HttpRequest.newBuilder(URI.create(listUrl))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()
    }

    internal fun listPage(page: Int, listRequestContext: ListRequestContext): HttpRequest {
        return HttpRequest.newBuilder(URI.create(LIST_AJAX_URL))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", FORM_URLENCODED)
            .header("Origin", DANAWA_ORIGIN)
            .header("Referer", listRequestContext.listUrl)
            .header("X-Requested-With", "XMLHttpRequest")
            .POST(HttpRequest.BodyPublishers.ofString(buildFormData(listRequestContext.toFormData(page))))
            .build()
    }

    internal fun detailPage(detailPage: String): HttpRequest {
        return HttpRequest.newBuilder(URI.create(detailPage))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()
    }

    internal fun detailSpec(
        productCard: ProductCard,
        detailRequestContext: DetailRequestContext?,
    ): HttpRequest? {
        val context = detailRequestContext?.takeIf { it.isComplete() } ?: return null
        val formData = linkedMapOf(
            "pcode" to productCard.productCode,
            "cate1" to productCard.cate1,
            "cate2" to productCard.cate2,
            "cate3" to productCard.cate3,
            "cate4" to productCard.cate4,
            "makerName" to context.makerName,
            "productName" to context.productName,
            "prodType" to context.prodType,
        )

        return HttpRequest.newBuilder(URI.create(PRODUCT_DESCRIPTION_URL))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", FORM_URLENCODED)
            .header("Origin", DANAWA_ORIGIN)
            .header("Referer", productCard.detailPage)
            .header("X-Requested-With", "XMLHttpRequest")
            .POST(HttpRequest.BodyPublishers.ofString(buildFormData(formData)))
            .build()
    }

    private fun DetailRequestContext.isComplete(): Boolean {
        return !makerName.isNullOrBlank() &&
            !productName.isNullOrBlank() &&
            !prodType.isNullOrBlank()
    }

    private fun buildFormData(data: Iterable<Pair<String, String?>>): String {
        return data
            .filter { !it.second.isNullOrBlank() }
            .joinToString("&") { (key, value) ->
                "${key.urlEncode()}=${value.orEmpty().urlEncode()}"
            }
    }

    private fun buildFormData(data: Map<String, String?>): String {
        return buildFormData(data.entries.map { it.key to it.value })
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8)
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
        const val LIST_AJAX_URL = "https://prod.danawa.com/list/ajax/getProductList.ajax.php"
        const val DANAWA_ORIGIN = "https://prod.danawa.com"
        const val PRODUCT_DESCRIPTION_URL = "https://prod.danawa.com/info/ajax/getProductDescription.ajax.php"
        const val FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8"
        val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(20)
    }
}
