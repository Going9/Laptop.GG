package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.LoadExistingCrawledLaptopLookupUseCase
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import going9.laptopgg.job.crawler.detail.ProductDetailCrawler
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.toCommand
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
internal class CrawlProductBatchProcessor(
    private val loadExistingLookupUseCase: LoadExistingCrawledLaptopLookupUseCase,
    private val detailCrawler: ProductDetailCrawler,
    private val snapshotSaver: CrawlProductSnapshotSaver,
    private val detailRefreshOutcomeHandler: DetailRefreshOutcomeHandler,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    internal fun process(
        productCards: List<ProductCard>,
        progress: CrawlProgress,
        detailFetchExecutor: DetailFetchExecutor,
    ): CrawlPageProcessingResult {
        progress.recordProcessed(productCards.size)
        if (productCards.isEmpty()) {
            return CrawlPageProcessingResult(processedCount = 0)
        }

        var pagePriceOnlyUpdatedCount = 0
        val existingLookup = loadExistingLookupUseCase.load(productCards.map { it.toCommand() })
        val workPlan = DetailRefreshPlanner.plan(productCards, existingLookup, now())

        for (workItem in workPlan.priceOnlySnapshotWorkItems) {
            pagePriceOnlyUpdatedCount += snapshotSaver.saveListSnapshot(workItem.existingLaptop.id, workItem.productCard, progress)
        }

        progress.recordDetailRefresh(workPlan.detailRefreshWorkItems.size)
        if (workPlan.detailRefreshWorkItems.isNotEmpty()) {
            pagePriceOnlyUpdatedCount += detailRefreshOutcomeHandler.handle(
                detailRefreshOutcomes = detailCrawler.fetchDetailRefreshOutcomes(
                    workPlan.detailRefreshWorkItems,
                    detailFetchExecutor,
                ),
                progress = progress,
            ).pagePriceOnlyUpdatedCount
        }

        return CrawlPageProcessingResult(
            processedCount = productCards.size,
            detailRefreshCount = workPlan.detailRefreshWorkItems.size,
            pagePriceOnlyUpdatedCount = pagePriceOnlyUpdatedCount,
        )
    }
}

internal data class CrawlPageProcessingResult(
    val processedCount: Int,
    val detailRefreshCount: Int = 0,
    val pagePriceOnlyUpdatedCount: Int = 0,
)
