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

    fun containsSubscriptionMarker(vararg texts: String?): Boolean {
        val normalized = texts
            .filterNotNull()
            .joinToString(" ")
            .lowercase()

        return normalized.isNotBlank() &&
            subscriptionMarkers.any { marker -> normalized.contains(marker) }
    }
}
