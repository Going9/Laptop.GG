package going9.laptopgg.job.crawler.danawa

internal object DanawaEndpoints {
    const val ORIGIN = "https://prod.danawa.com"
    const val NOTEBOOK_LIST_URL = "$ORIGIN/list/?cate=112758"
    const val APPLE_MACBOOK_LIST_URL = "$ORIGIN/list/?cate=11236463"
    const val LIST_AJAX_URL = "$ORIGIN/list/ajax/getProductList.ajax.php"
    const val PRODUCT_DESCRIPTION_URL = "$ORIGIN/info/ajax/getProductDescription.ajax.php"

    fun productDetailUrl(productCode: String, categoryCode: String): String {
        return "$ORIGIN/info/?pcode=$productCode&cate=$categoryCode"
    }
}
