package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductPageBatch
import going9.laptopgg.job.crawler.source.CrawlSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlSourceStopDecisionLogger(
    private val crawlPageDiagnosticsLogger: CrawlPageDiagnosticsLogger,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun log(
        stopDecision: CrawlSourceStopDecision,
        crawlSource: CrawlSource,
        currentPage: Int,
        pageBatch: ProductPageBatch,
        pageAnalysis: CrawlPageAnalysis,
        diagnosticContext: CrawlPageDiagnosticContext,
    ) {
        when (stopDecision.reason) {
            CrawlSourceStopReason.REACHED_LIMIT, null -> Unit
            CrawlSourceStopReason.EXPECTED_LAST_PAGE -> logger.info(
                "총 상품 수 기준 마지막 페이지에 도달해 크롤링을 종료합니다. source={}, page={}, priceCompareCount={}, expectedLastPage={}, hasNextPage={}",
                crawlSource.key,
                currentPage,
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
                currentPage,
            )
        }
    }
}
