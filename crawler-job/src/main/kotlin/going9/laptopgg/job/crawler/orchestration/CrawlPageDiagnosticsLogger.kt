package going9.laptopgg.job.crawler.orchestration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class CrawlPageDiagnosticsLogger(
    private val formatter: CrawlPageDiagnosticFormatter,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun shouldLogPageDiagnostics(
        page: Int,
        freshProductCount: Int,
        repeatedPageSignature: Boolean,
    ): Boolean {
        return page == 1 || freshProductCount == 0 || repeatedPageSignature
    }

    internal fun logPageDiagnostics(context: CrawlPageDiagnosticContext) {
        val diagnostics = formatter.format(context)
        logger.info(
            "페이지 진단: source={}, page={}, hasNextPage={}, priceCompareCount={}, expectedLastPage={}, visiblePages={}, nextPageHint={}, repeatedPageSignature={}, pageSignatureHash={}, firstCard={}, lastCard={}, requestPage={}, requestSortMethod={}, requestFilterCount={}, requestDistinctFilterCount={}",
            context.sourceKey,
            context.page,
            context.pageBatch.hasNextPage,
            diagnostics.priceCompareCount,
            diagnostics.expectedLastPage,
            diagnostics.visiblePages,
            diagnostics.nextPageHint,
            context.repeatedPageSignature,
            diagnostics.pageSignatureHash,
            diagnostics.firstCard,
            diagnostics.lastCard,
            context.page,
            context.requestSortMethod,
            context.requestFilterCount,
            context.requestDistinctFilterCount,
        )
    }

    internal fun logPageProcessing(
        context: CrawlPageDiagnosticContext,
        pageDurationMillis: Long,
        freshProductCount: Int,
        pageProcessingResult: CrawlPageProcessingResult,
        duplicateSkippedCount: Int,
        progress: CrawlProgress,
    ) {
        logger.info(
            "페이지 처리 시간: ${pageDurationMillis}ms / source=${context.sourceKey} / page=${context.page} / " +
                "수집 상품: ${context.productCards.size}개 / 신규 상품: ${freshProductCount}개 / 실제 처리: ${pageProcessingResult.processedCount}개 / " +
                "상세 재수집: ${pageProcessingResult.detailRefreshCount}개 / 중복 스킵: ${duplicateSkippedCount}개 / " +
                "가격만 갱신(페이지): ${pageProcessingResult.pagePriceOnlyUpdatedCount}개 / 가격만 갱신(누적): ${progress.priceOnlyUpdatedCount}개 / " +
                "누적 처리: ${progress.processedCount}개 / 누적 열화: ${progress.degradedCount}개 / 누적 실패: ${progress.failedCount}개",
        )
    }

    internal fun logDuplicateTailStop(
        context: CrawlPageDiagnosticContext,
        consecutiveDuplicateOnlyPages: Int,
    ) {
        val diagnostics = formatter.format(context)
        logger.info(
            "AJAX 페이지네이션에서도 새 detail 페이지가 없는 반복 목록이 이어져 크롤링을 종료합니다. source={}, page={}, repeatedPageSignature={}, consecutiveDuplicateOnlyPages={}, hasNextPage={}, visiblePages={}, nextPageHint={}, priceCompareCount={}, expectedLastPage={}, pageSignatureHash={}, firstCard={}, lastCard={}, requestPage={}, requestSortMethod={}, requestFilterCount={}, requestDistinctFilterCount={}",
            context.sourceKey,
            context.page,
            context.repeatedPageSignature,
            consecutiveDuplicateOnlyPages,
            context.pageBatch.hasNextPage,
            diagnostics.visiblePages,
            diagnostics.nextPageHint,
            diagnostics.priceCompareCount,
            diagnostics.expectedLastPage,
            diagnostics.pageSignatureHash,
            diagnostics.firstCard,
            diagnostics.lastCard,
            context.page,
            context.requestSortMethod,
            context.requestFilterCount,
            context.requestDistinctFilterCount,
        )
    }
}
