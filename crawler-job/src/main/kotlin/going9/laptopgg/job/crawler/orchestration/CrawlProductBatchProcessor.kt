package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.job.crawler.detail.BuildLaptopResult
import going9.laptopgg.job.crawler.detail.DetailCrawler
import going9.laptopgg.job.crawler.detail.DetailRefreshOutcome
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.toCommand
import java.util.concurrent.ExecutorService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlProductBatchProcessor(
    private val saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase,
    private val detailCrawler: DetailCrawler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun process(
        productCards: List<ProductCard>,
        progress: CrawlProgress,
        detailFetchExecutor: ExecutorService,
    ): CrawlPageProcessingResult {
        progress.recordProcessed(productCards.size)
        if (productCards.isEmpty()) {
            return CrawlPageProcessingResult(processedCount = 0)
        }

        var pagePriceOnlyUpdatedCount = 0
        val existingLookup = saveCrawledLaptopUseCase.loadExistingLookup(productCards.map { it.toCommand() })
        val workPlan = DetailRefreshPlanner.plan(productCards, existingLookup)

        for (workItem in workPlan.priceOnlySnapshotWorkItems) {
            pagePriceOnlyUpdatedCount += saveListSnapshot(workItem.existingLaptop.id, workItem.productCard, progress)
        }

        progress.recordDetailRefresh(workPlan.detailRefreshWorkItems.size)
        if (workPlan.detailRefreshWorkItems.isNotEmpty()) {
            processDetailRefreshOutcomes(
                detailRefreshOutcomes = detailCrawler.fetchDetailRefreshOutcomes(
                    workPlan.detailRefreshWorkItems,
                    detailFetchExecutor,
                ),
                progress = progress,
                pagePriceOnlyUpdatedCount = { pagePriceOnlyUpdatedCount++ },
            )
        }

        return CrawlPageProcessingResult(
            processedCount = productCards.size,
            detailRefreshCount = workPlan.detailRefreshWorkItems.size,
            pagePriceOnlyUpdatedCount = pagePriceOnlyUpdatedCount,
        )
    }

    private fun processDetailRefreshOutcomes(
        detailRefreshOutcomes: List<DetailRefreshOutcome>,
        progress: CrawlProgress,
        pagePriceOnlyUpdatedCount: () -> Unit,
    ) {
        for (detailRefreshOutcome in detailRefreshOutcomes) {
            val productCard = detailRefreshOutcome.workItem.productCard
            val existingLaptop = detailRefreshOutcome.workItem.existingLaptop
            try {
                val buildResult = detailRefreshOutcome.buildResult
                if (buildResult != null) {
                    if (buildResult.isDegraded) {
                        progress.recordDegraded(
                            productCard = productCard,
                            reason = buildResult.degradationReasons.joinToString(" | "),
                        )
                        logger.warn(
                            "상품 일부 스펙을 요약/기존값으로 보완했습니다. productCode={}, detailPage={}, reasons={}",
                            productCard.productCode,
                            productCard.detailPage,
                            buildResult.degradationReasons,
                        )
                    }

                    progress.recordSaveResult(saveCrawledLaptopUseCase.saveOrUpdateLaptop(buildResult.command, existingLaptop?.id))
                    continue
                }

                if (existingLaptop != null) {
                    if (saveListSnapshot(existingLaptop.id, productCard, progress) > 0) {
                        pagePriceOnlyUpdatedCount()
                    }
                }

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
            } catch (e: Exception) {
                progress.recordFailure(
                    productCard = productCard,
                    reason = e.message ?: e::class.simpleName ?: "알 수 없는 오류",
                )
                logger.error("상품 크롤링 저장 중 오류 발생. productCode={}, detailPage={}", productCard.productCode, productCard.detailPage, e)
            }
        }
    }

    private fun saveListSnapshot(
        existingLaptopId: Long,
        productCard: ProductCard,
        progress: CrawlProgress,
    ): Int {
        return try {
            val saveResult = saveCrawledLaptopUseCase.saveListSnapshot(existingLaptopId, productCard.toCommand())
            if (progress.recordPriceOnlySaveResult(saveResult)) 1 else 0
        } catch (e: Exception) {
            progress.recordFailure(
                productCard = productCard,
                reason = e.message ?: e::class.simpleName ?: "알 수 없는 오류",
            )
            logger.error(
                "기존 상품 가격/목록 스냅샷 업데이트 중 오류 발생. productCode={}, detailPage={}",
                productCard.productCode,
                productCard.detailPage,
                e,
            )
            0
        }
    }
}

internal data class CrawlPageProcessingResult(
    val processedCount: Int,
    val detailRefreshCount: Int = 0,
    val pagePriceOnlyUpdatedCount: Int = 0,
)
