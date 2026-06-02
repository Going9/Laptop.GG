package going9.laptopgg.job.crawler.orchestration

internal object DuplicateTailStopPolicy {
    fun shouldStop(
        freshProductCount: Int,
        consecutiveDuplicateOnlyPages: Int,
    ): Boolean {
        if (freshProductCount > 0) {
            return false
        }

        return consecutiveDuplicateOnlyPages >= MAX_CONSECUTIVE_DUPLICATE_ONLY_PAGES
    }

    private const val MAX_CONSECUTIVE_DUPLICATE_ONLY_PAGES = 5
}
