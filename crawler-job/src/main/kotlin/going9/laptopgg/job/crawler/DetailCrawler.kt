package going9.laptopgg.job.crawler

import java.util.concurrent.ExecutorService
import org.springframework.stereotype.Component

@Component
class DetailCrawler(
    private val danawaClient: DanawaClient,
    private val laptopSnapshotMerger: LaptopSnapshotMerger,
) {
    internal fun fetchDetailRefreshOutcomes(
        workItems: List<DetailRefreshWorkItem>,
        executor: ExecutorService,
    ): List<DetailRefreshOutcome> {
        if (workItems.isEmpty()) {
            return emptyList()
        }

        return workItems.map { workItem ->
            executor.submit<DetailRefreshOutcome> {
                runCatching {
                    DetailRefreshOutcome(
                        workItem = workItem,
                        buildResult = buildLaptop(workItem.productCard),
                    )
                }.getOrElse { throwable ->
                    DetailRefreshOutcome(
                        workItem = workItem,
                        error = throwable as? Exception ?: IllegalStateException(throwable.message, throwable),
                    )
                }
            }
        }.map { future ->
            future.get()
        }
    }

    private fun buildLaptop(productCard: ProductCard): BuildLaptopResult {
        val degradationReasons = mutableListOf<String>()
        val detailPageHtml = danawaClient.fetchDetailPage(productCard.detailPage)
        val detailContext = DanawaDetailParser.extractDetailRequestContext(detailPageHtml)
        if (detailContext == null) {
            degradationReasons += "상세 스펙 요청 컨텍스트 없음"
        }

        val detailSpecHtml = danawaClient.fetchDetailSpec(productCard, detailContext)
        if (detailSpecHtml == null) {
            degradationReasons += "상세 스펙 테이블 미수집"
        }

        val parsedSpecTable = detailSpecHtml?.let(DanawaDetailParser::parseSpecTable) ?: ParsedSpecTable(emptyMap(), emptyList())
        if (detailSpecHtml != null && parsedSpecTable.values.isEmpty()) {
            degradationReasons += "상세 스펙 테이블 파싱 결과 비어 있음"
        }

        val summaryFallback = DanawaDetailParser.parseSummaryFallback(DanawaDetailParser.extractSummaryText(detailPageHtml))
        if (parsedSpecTable.values.isEmpty() && DanawaDetailParser.isEmpty(summaryFallback)) {
            degradationReasons += "상세/요약 스펙 모두 비어 있음"
        }

        return BuildLaptopResult(
            laptop = laptopSnapshotMerger.createLaptop(productCard, parsedSpecTable, summaryFallback),
            degradationReasons = degradationReasons.distinct(),
        )
    }
}
