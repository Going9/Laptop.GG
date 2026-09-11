package going9.laptopgg.job.crawler.danawa

internal object DanawaSubscriptionProductDetector {
    private val subscriptionMarkers = listOf(
        "가전 구독",
        "구독/렌탈",
        "구독형",
        "렌탈",
        "임대",
        "대여",
        "의무사용",
        "자가관리",
        "설치비포함",
        "subscription",
        "rental",
        "lease",
    )

    private val nonRetailMarkers = listOf(
        "중고",
        "리퍼",
        "전시상품",
        "반품",
    )

    fun containsSubscriptionMarker(vararg texts: String?): Boolean {
        return containsMarker(subscriptionMarkers, texts)
    }

    fun containsExcludedProductMarker(vararg texts: String?): Boolean {
        return containsMarker(subscriptionMarkers + nonRetailMarkers, texts)
    }

    private fun containsMarker(markers: List<String>, texts: Array<out String?>): Boolean {
        val normalized = texts
            .filterNotNull()
            .joinToString(" ")
            .lowercase()

        return normalized.isNotBlank() &&
            markers.any { marker -> normalized.contains(marker) }
    }
}
