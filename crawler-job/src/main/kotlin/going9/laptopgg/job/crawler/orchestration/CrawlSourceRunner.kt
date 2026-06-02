package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductListPageCrawler
import going9.laptopgg.job.crawler.source.CrawlSource
import going9.laptopgg.job.crawler.support.isCrawlerInterruptedFailure
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

internal interface CrawlSourceRunUseCase {
    fun runSource(
        crawlSource: CrawlSource,
        startPage: Int,
        runContext: CrawlRunContext,
    ): CrawlSourceRunResult
}

@Component
internal class CrawlSourceRunner(
    private val listPageCrawler: ProductListPageCrawler,
    private val crawlProductBatchProcessor: CrawlProductBatchProcessor,
    private val crawlPageDiagnosticsLogger: CrawlPageDiagnosticsLogger,
    private val stopDecisionLogger: CrawlSourceStopDecisionLogger,
    private val crawlClock: CrawlClock,
) : CrawlSourceRunUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun runSource(
        crawlSource: CrawlSource,
        startPage: Int,
        runContext: CrawlRunContext,
    ): CrawlSourceRunResult {
        val progress = runContext.progress
        val listRequestContext = try {
            listPageCrawler.createListRequestContext(crawlSource)
        } catch (exception: Exception) {
            if (exception.isCrawlerInterruptedFailure()) {
                throw exception
            }
            progress.recordSourceFailure(crawlSource.key, exception.toFailureReason())
            logger.error("크롤 소스 요청 컨텍스트 생성에 실패했습니다. source={}", crawlSource.key, exception)
            return CrawlSourceRunResult(reachedLimit = false, hitMaxListPages = false)
        }
        val requestFilterCount = listRequestContext.searchAttributeValues.size
        val requestDistinctFilterCount = listRequestContext.searchAttributeValues.toSet().size
        val traversalState = runContext.traversalState(startPage)

        logger.info(
            "크롤 소스를 시작합니다. source={}, startPage={}, attributeFilterCount={}, filters={}",
            crawlSource.key,
            traversalState.currentPage,
            crawlSource.attributeFilters.size,
            crawlSource.attributeFilters.joinToString { it.name }.ifBlank { "없음" },
        )

        while (traversalState.currentPage <= runContext.maxListPages) {
            val pageStartTime = crawlClock.currentTimeMillis()
            val pageBatch = try {
                listPageCrawler.fetchProductPageBatch(traversalState.currentPage, listRequestContext)
            } catch (exception: Exception) {
                if (exception.isCrawlerInterruptedFailure()) {
                    throw exception
                }
                progress.recordPageFailure(crawlSource.key, traversalState.currentPage, exception.toFailureReason())
                logger.error(
                    "목록 페이지 수집에 실패해 현재 소스를 중단합니다. source={}, page={}",
                    crawlSource.key,
                    traversalState.currentPage,
                    exception,
                )
                break
            }
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

            val remainingQuota = progress.remainingQuota(runContext.limit)
            if (remainingQuota == 0) {
                break
            }

            val candidateProductCards = remainingQuota?.let(pageAnalysis.freshProductCards::take) ?: pageAnalysis.freshProductCards
            val pageProcessingResult = crawlProductBatchProcessor.process(
                productCards = candidateProductCards,
                progress = progress,
                detailFetchExecutor = runContext.detailFetchExecutor,
            )

            if (progress.reachedLimit(runContext.limit)) {
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
                pageDurationMillis = crawlClock.currentTimeMillis() - pageStartTime,
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
                stopDecisionLogger.log(
                    stopDecision = stopDecision,
                    crawlSource = crawlSource,
                    currentPage = traversalState.currentPage,
                    pageBatch = pageBatch,
                    pageAnalysis = pageAnalysis,
                    diagnosticContext = diagnosticContext,
                )
                break
            }

            traversalState.advance()
        }

        return traversalState.toRunResult(runContext.maxListPages)
    }

    private fun Exception.toFailureReason(): String {
        return message ?: javaClass.simpleName
    }

}

internal data class CrawlSourceRunResult(
    val reachedLimit: Boolean,
    val hitMaxListPages: Boolean,
)
