package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ListPageCrawler
import going9.laptopgg.job.crawler.source.CrawlSource
import java.util.concurrent.ExecutorService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlSourceRunner(
    private val listPageCrawler: ListPageCrawler,
    private val crawlProductBatchProcessor: CrawlProductBatchProcessor,
    private val crawlPageDiagnosticsLogger: CrawlPageDiagnosticsLogger,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun runSource(
        crawlSource: CrawlSource,
        startPage: Int,
        maxListPages: Int,
        limit: Int?,
        seenDetailPages: MutableSet<String>,
        progress: CrawlProgress,
        detailFetchExecutor: ExecutorService,
    ): CrawlSourceRunResult {
        val listRequestContext = listPageCrawler.createListRequestContext(crawlSource)
        val requestFilterCount = listRequestContext.searchAttributeValues.size
        val requestDistinctFilterCount = listRequestContext.searchAttributeValues.toSet().size
        val seenPageSignatures = linkedSetOf<String>()
        var currentPage = startPage.coerceAtLeast(1)
        var consecutiveDuplicateOnlyPages = 0
        var reachedLimit = false

        logger.info(
            "크롤 소스를 시작합니다. source={}, startPage={}, attributeFilterCount={}, filters={}",
            crawlSource.key,
            currentPage,
            crawlSource.attributeFilters.size,
            crawlSource.attributeFilters.joinToString { it.name }.ifBlank { "없음" },
        )

        while (currentPage <= maxListPages) {
            val pageStartTime = System.currentTimeMillis()
            val pageBatch = listPageCrawler.fetchProductPageBatch(currentPage, listRequestContext)
            val productCards = pageBatch.productCards
            val expectedLastPage = pageBatch.priceCompareCount
                ?.takeIf { it > 0 }
                ?.let { ((it - 1) / pageBatch.productCards.size.coerceAtLeast(1)) + 1 }

            if (productCards.isEmpty()) {
                logger.info("현재 페이지에서 수집 가능한 상품이 없어 크롤링을 종료합니다. source={}, page={}", crawlSource.key, currentPage)
                break
            }

            val pageSignature = ProductPageSignature.create(productCards)
            val isRepeatedPageSignature = !seenPageSignatures.add(pageSignature)
            val freshProductCards = productCards.filter { seenDetailPages.add(it.detailPage) }
            val duplicateSkippedCount = productCards.size - freshProductCards.size
            val diagnosticContext = CrawlPageDiagnosticContext(
                sourceKey = crawlSource.key,
                page = currentPage,
                pageBatch = pageBatch,
                productCards = productCards,
                expectedLastPage = expectedLastPage,
                repeatedPageSignature = isRepeatedPageSignature,
                pageSignature = pageSignature,
                requestSortMethod = listRequestContext.sortMethod,
                requestFilterCount = requestFilterCount,
                requestDistinctFilterCount = requestDistinctFilterCount,
            )
            consecutiveDuplicateOnlyPages = if (freshProductCards.isEmpty()) {
                consecutiveDuplicateOnlyPages + 1
            } else {
                0
            }

            val remainingQuota = progress.remainingQuota(limit)
            if (remainingQuota == 0) {
                break
            }

            val candidateProductCards = remainingQuota?.let(freshProductCards::take) ?: freshProductCards
            val pageProcessingResult = crawlProductBatchProcessor.process(
                productCards = candidateProductCards,
                progress = progress,
                detailFetchExecutor = detailFetchExecutor,
            )

            if (progress.reachedLimit(limit)) {
                reachedLimit = true
            }

            if (crawlPageDiagnosticsLogger.shouldLogPageDiagnostics(
                    page = currentPage,
                    freshProductCount = freshProductCards.size,
                    repeatedPageSignature = isRepeatedPageSignature,
                )
            ) {
                crawlPageDiagnosticsLogger.logPageDiagnostics(diagnosticContext)
            }

            crawlPageDiagnosticsLogger.logPageProcessing(
                context = diagnosticContext,
                pageDurationMillis = System.currentTimeMillis() - pageStartTime,
                freshProductCount = freshProductCards.size,
                pageProcessingResult = pageProcessingResult,
                duplicateSkippedCount = duplicateSkippedCount,
                progress = progress,
            )

            if (reachedLimit) {
                break
            }

            if (expectedLastPage != null && currentPage >= expectedLastPage) {
                logger.info(
                    "총 상품 수 기준 마지막 페이지에 도달해 크롤링을 종료합니다. source={}, page={}, priceCompareCount={}, expectedLastPage={}, hasNextPage={}",
                    crawlSource.key,
                    currentPage,
                    pageBatch.priceCompareCount,
                    expectedLastPage,
                    pageBatch.hasNextPage,
                )
                break
            }

            if (DuplicateTailStopPolicy.shouldStop(
                    freshProductCount = freshProductCards.size,
                    consecutiveDuplicateOnlyPages = consecutiveDuplicateOnlyPages,
                )
            ) {
                crawlPageDiagnosticsLogger.logDuplicateTailStop(diagnosticContext, consecutiveDuplicateOnlyPages)
                break
            }

            if (!pageBatch.hasNextPage) {
                logger.info("다음 페이지가 없어 크롤링을 종료합니다. source={}, page={}", crawlSource.key, currentPage)
                break
            }

            currentPage++
        }

        return CrawlSourceRunResult(
            reachedLimit = reachedLimit,
            hitMaxListPages = currentPage > maxListPages,
        )
    }
}

internal data class CrawlSourceRunResult(
    val reachedLimit: Boolean,
    val hitMaxListPages: Boolean,
)
