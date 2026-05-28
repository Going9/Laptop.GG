package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.detail.DetailRefreshOutcome
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.support.isCrawlerInterruptedFailure
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class DetailRefreshOutcomeHandler(
    private val snapshotSaver: CrawlProductSnapshotSaver,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun handle(
        detailRefreshOutcomes: List<DetailRefreshOutcome>,
        progress: CrawlProgress,
    ): DetailRefreshOutcomeProcessingResult {
        var pagePriceOnlyUpdatedCount = 0
        for (detailRefreshOutcome in detailRefreshOutcomes) {
            pagePriceOnlyUpdatedCount += handleOutcome(detailRefreshOutcome, progress)
        }
        return DetailRefreshOutcomeProcessingResult(pagePriceOnlyUpdatedCount)
    }

    private fun handleOutcome(
        detailRefreshOutcome: DetailRefreshOutcome,
        progress: CrawlProgress,
    ): Int {
        val productCard = detailRefreshOutcome.workItem.productCard
        val existingLaptop = detailRefreshOutcome.workItem.existingLaptop

        return try {
            val buildResult = detailRefreshOutcome.buildResult
            if (buildResult != null) {
                if (buildResult.isDegraded) {
                    recordDegradedBuild(productCard, buildResult.degradationReasons, progress)
                }
                snapshotSaver.saveDetailSnapshot(buildResult, existingLaptop?.id, progress)
                return 0
            }

            val priceOnlyUpdatedCount = existingLaptop?.let {
                snapshotSaver.saveListSnapshot(it.id, productCard, progress)
            } ?: 0

            val error = detailRefreshOutcome.error
            progress.recordFailure(
                productCard = productCard,
                reason = error?.message ?: error?.javaClass?.simpleName ?: "알 수 없는 오류",
            )
            logger.error(
                "상품 상세 재수집 중 오류 발생. productCode={}, detailPage={}",
                productCard.productCode,
                productCard.detailPage,
                error,
            )

            priceOnlyUpdatedCount
        } catch (e: Exception) {
            if (e.isCrawlerInterruptedFailure()) {
                throw e
            }
            progress.recordFailure(
                productCard = productCard,
                reason = e.message ?: e::class.simpleName ?: "알 수 없는 오류",
            )
            logger.error("상품 크롤링 저장 중 오류 발생. productCode={}, detailPage={}", productCard.productCode, productCard.detailPage, e)
            0
        }
    }

    private fun recordDegradedBuild(
        productCard: ProductCard,
        degradationReasons: List<String>,
        progress: CrawlProgress,
    ) {
        progress.recordDegraded(
            productCard = productCard,
            reason = degradationReasons.joinToString(" | "),
        )
        logger.warn(
            "상품 일부 스펙을 요약/기존값으로 보완했습니다. productCode={}, detailPage={}, reasons={}",
            productCard.productCode,
            productCard.detailPage,
            degradationReasons,
        )
    }
}

internal data class DetailRefreshOutcomeProcessingResult(
    val pagePriceOnlyUpdatedCount: Int = 0,
)
