package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductPageBatch

internal object CrawlSourceStopPolicy {
    fun decideAfterProcessedPage(
        pageBatch: ProductPageBatch,
        pageAnalysis: CrawlPageAnalysis,
        currentPage: Int,
        reachedLimit: Boolean,
    ): CrawlSourceStopDecision {
        if (reachedLimit) {
            return CrawlSourceStopDecision(CrawlSourceStopReason.REACHED_LIMIT)
        }

        if (pageAnalysis.expectedLastPage != null && currentPage >= pageAnalysis.expectedLastPage) {
            return CrawlSourceStopDecision(CrawlSourceStopReason.EXPECTED_LAST_PAGE)
        }

        if (
            DuplicateTailStopPolicy.shouldStop(
                freshProductCount = pageAnalysis.freshProductCards.size,
                consecutiveDuplicateOnlyPages = pageAnalysis.consecutiveDuplicateOnlyPages,
            )
        ) {
            return CrawlSourceStopDecision(CrawlSourceStopReason.DUPLICATE_TAIL)
        }

        if (!pageBatch.hasNextPage) {
            return CrawlSourceStopDecision(CrawlSourceStopReason.NO_NEXT_PAGE)
        }

        return CrawlSourceStopDecision.CONTINUE
    }
}

internal data class CrawlSourceStopDecision(
    val reason: CrawlSourceStopReason?,
) {
    val shouldStop: Boolean
        get() = reason != null

    companion object {
        val CONTINUE = CrawlSourceStopDecision(null)
    }
}

internal enum class CrawlSourceStopReason {
    REACHED_LIMIT,
    EXPECTED_LAST_PAGE,
    DUPLICATE_TAIL,
    NO_NEXT_PAGE,
}
