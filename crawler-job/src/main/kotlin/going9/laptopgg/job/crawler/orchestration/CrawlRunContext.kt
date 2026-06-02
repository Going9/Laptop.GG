package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.detail.DetailFetchExecutor

internal class CrawlRunContext(
    val maxListPages: Int,
    val limit: Int?,
    val progress: CrawlProgress,
    val detailFetchExecutor: DetailFetchExecutor,
    private val seenDetailPages: MutableSet<String> = linkedSetOf(),
) {
    fun traversalState(startPage: Int): CrawlSourceTraversalState {
        return CrawlSourceTraversalState(startPage, seenDetailPages)
    }
}
