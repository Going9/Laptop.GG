package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ListPageCrawler
import going9.laptopgg.job.crawler.list.ProductPageBatch
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
        val traversalState = CrawlSourceTraversalState(startPage, seenDetailPages)

        logger.info(
            "크롤 소스를 시작합니다. source={}, startPage={}, attributeFilterCount={}, filters={}",
            crawlSource.key,
            traversalState.currentPage,
            crawlSource.attributeFilters.size,
            crawlSource.attributeFilters.joinToString { it.name }.ifBlank { "없음" },
        )

        while (traversalState.currentPage <= maxListPages) {
            val pageStartTime = System.currentTimeMillis()
            val pageBatch = listPageCrawler.fetchProductPageBatch(traversalState.currentPage, listRequestContext)
            val pageAnalysis = traversalState.analyze(pageBatch)

            if (pageAnalysis.productCards.isEmpty()) {
                logger.info("현재 페이지에서 수집 가능한 상품이 없어 크롤링을 종료합니다. source={}, page={}", crawlSource.key, traversalState.currentPage)
                break
            }

            val diagnosticContext = CrawlPageDiagnosticContext(
                sourceKey = crawlSource.key,
                page = traversalState.currentPage,
                pageBatch = pageBatch,
                productCards = pageAnalysis.productCards,
                expectedLastPage = pageAnalysis.expectedLastPage,
                repeatedPageSignature = pageAnalysis.repeatedPageSignature,
                pageSignature = pageAnalysis.pageSignature,
                requestSortMethod = listRequestContext.sortMethod,
                requestFilterCount = requestFilterCount,
                requestDistinctFilterCount = requestDistinctFilterCount,
            )

            val remainingQuota = progress.remainingQuota(limit)
            if (remainingQuota == 0) {
                break
            }

            val candidateProductCards = remainingQuota?.let(pageAnalysis.freshProductCards::take) ?: pageAnalysis.freshProductCards
            val pageProcessingResult = crawlProductBatchProcessor.process(
                productCards = candidateProductCards,
                progress = progress,
                detailFetchExecutor = detailFetchExecutor,
            )

            if (progress.reachedLimit(limit)) {
                traversalState.markReachedLimit()
            }

            if (crawlPageDiagnosticsLogger.shouldLogPageDiagnostics(
                    page = traversalState.currentPage,
                    freshProductCount = pageAnalysis.freshProductCards.size,
                    repeatedPageSignature = pageAnalysis.repeatedPageSignature,
                )
            ) {
                crawlPageDiagnosticsLogger.logPageDiagnostics(diagnosticContext)
            }

            crawlPageDiagnosticsLogger.logPageProcessing(
                context = diagnosticContext,
                pageDurationMillis = System.currentTimeMillis() - pageStartTime,
                freshProductCount = pageAnalysis.freshProductCards.size,
                pageProcessingResult = pageProcessingResult,
                duplicateSkippedCount = pageAnalysis.duplicateSkippedCount,
                progress = progress,
            )

            val stopDecision = CrawlSourceStopPolicy.decideAfterProcessedPage(
                pageBatch = pageBatch,
                pageAnalysis = pageAnalysis,
                currentPage = traversalState.currentPage,
                reachedLimit = traversalState.reachedLimit,
            )
            if (stopDecision.shouldStop) {
                logStopDecision(stopDecision, crawlSource, traversalState, pageBatch, pageAnalysis, diagnosticContext)
                break
            }

            traversalState.advance()
        }

        return traversalState.toRunResult(maxListPages)
    }

    private fun logStopDecision(
        stopDecision: CrawlSourceStopDecision,
        crawlSource: CrawlSource,
        traversalState: CrawlSourceTraversalState,
        pageBatch: ProductPageBatch,
        pageAnalysis: CrawlPageAnalysis,
        diagnosticContext: CrawlPageDiagnosticContext,
    ) {
        when (stopDecision.reason) {
            CrawlSourceStopReason.REACHED_LIMIT, null -> Unit
            CrawlSourceStopReason.EXPECTED_LAST_PAGE -> logger.info(
                "총 상품 수 기준 마지막 페이지에 도달해 크롤링을 종료합니다. source={}, page={}, priceCompareCount={}, expectedLastPage={}, hasNextPage={}",
                crawlSource.key,
                traversalState.currentPage,
                pageBatch.priceCompareCount,
                pageAnalysis.expectedLastPage,
                pageBatch.hasNextPage,
            )
            CrawlSourceStopReason.DUPLICATE_TAIL -> crawlPageDiagnosticsLogger.logDuplicateTailStop(
                diagnosticContext,
                pageAnalysis.consecutiveDuplicateOnlyPages,
            )
            CrawlSourceStopReason.NO_NEXT_PAGE -> logger.info(
                "다음 페이지가 없어 크롤링을 종료합니다. source={}, page={}",
                crawlSource.key,
                traversalState.currentPage,
            )
        }
    }
}

internal data class CrawlSourceRunResult(
    val reachedLimit: Boolean,
    val hitMaxListPages: Boolean,
)
