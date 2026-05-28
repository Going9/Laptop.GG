package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.ProductPageBatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlPageDiagnosticsLogger {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun shouldLogPageDiagnostics(
        page: Int,
        freshProductCount: Int,
        repeatedPageSignature: Boolean,
    ): Boolean {
        return page == 1 || freshProductCount == 0 || repeatedPageSignature
    }

    internal fun logPageDiagnostics(context: CrawlPageDiagnosticContext) {
        logger.info(
            "페이지 진단: source={}, page={}, hasNextPage={}, priceCompareCount={}, expectedLastPage={}, visiblePages={}, nextPageHint={}, repeatedPageSignature={}, pageSignatureHash={}, firstCard={}, lastCard={}, requestPage={}, requestSortMethod={}, requestFilterCount={}, requestDistinctFilterCount={}",
            context.sourceKey,
            context.page,
            context.pageBatch.hasNextPage,
            context.pageBatch.priceCompareCount ?: "알 수 없음",
            context.expectedLastPage ?: "알 수 없음",
            context.visiblePagesLog(),
            context.pageBatch.nextPageHint ?: "없음",
            context.repeatedPageSignature,
            ProductPageSignature.stableHash(context.pageSignature),
            describeCard(context.productCards.firstOrNull()),
            describeCard(context.productCards.lastOrNull()),
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
        logger.info(
            "AJAX 페이지네이션에서도 새 detail 페이지가 없는 반복 목록이 이어져 크롤링을 종료합니다. source={}, page={}, repeatedPageSignature={}, consecutiveDuplicateOnlyPages={}, hasNextPage={}, visiblePages={}, nextPageHint={}, priceCompareCount={}, expectedLastPage={}, pageSignatureHash={}, firstCard={}, lastCard={}, requestPage={}, requestSortMethod={}, requestFilterCount={}, requestDistinctFilterCount={}",
            context.sourceKey,
            context.page,
            context.repeatedPageSignature,
            consecutiveDuplicateOnlyPages,
            context.pageBatch.hasNextPage,
            context.visiblePagesLog(),
            context.pageBatch.nextPageHint ?: "없음",
            context.pageBatch.priceCompareCount ?: "알 수 없음",
            context.expectedLastPage ?: "알 수 없음",
            ProductPageSignature.stableHash(context.pageSignature),
            describeCard(context.productCards.firstOrNull()),
            describeCard(context.productCards.lastOrNull()),
            context.page,
            context.requestSortMethod,
            context.requestFilterCount,
            context.requestDistinctFilterCount,
        )
    }

    private fun CrawlPageDiagnosticContext.visiblePagesLog(): String {
        return pageBatch.visiblePageNumbers
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
            ?: "없음"
    }

    private fun describeCard(productCard: ProductCard?): String {
        if (productCard == null) {
            return "없음"
        }

        val cate = extractQueryParam(productCard.detailPage, "cate") ?: productCard.cate4
        return "${productCard.productCode}@${cate}"
    }

    private fun extractQueryParam(url: String, key: String): String? {
        return Regex("""(?:\?|&)$key=([^&#]+)""").find(url)?.groupValues?.getOrNull(1)
    }
}

internal data class CrawlPageDiagnosticContext(
    val sourceKey: String,
    val page: Int,
    val pageBatch: ProductPageBatch,
    val productCards: List<ProductCard>,
    val expectedLastPage: Int?,
    val repeatedPageSignature: Boolean,
    val pageSignature: String,
    val requestSortMethod: String,
    val requestFilterCount: Int,
    val requestDistinctFilterCount: Int,
)
