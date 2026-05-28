package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.job.crawler.detail.BuildLaptopResult
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.toCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CrawlProductSnapshotSaver(
    private val saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun saveDetailSnapshot(
        buildResult: BuildLaptopResult,
        existingLaptopId: Long?,
        progress: CrawlProgress,
    ) {
        progress.recordSaveResult(saveCrawledLaptopUseCase.saveOrUpdateLaptop(buildResult.command, existingLaptopId))
    }

    internal fun saveListSnapshot(
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
