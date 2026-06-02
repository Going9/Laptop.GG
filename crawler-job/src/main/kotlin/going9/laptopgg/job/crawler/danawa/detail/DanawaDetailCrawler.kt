package going9.laptopgg.job.crawler.danawa.detail

import going9.laptopgg.job.crawler.detail.BuildLaptopResult
import going9.laptopgg.job.crawler.detail.DetailFetchExecutor
import going9.laptopgg.job.crawler.detail.DetailRefreshOutcome
import going9.laptopgg.job.crawler.detail.DetailRefreshWorkItem
import going9.laptopgg.job.crawler.detail.ProductDetailCrawler
import going9.laptopgg.job.crawler.danawa.client.DanawaClient
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.support.isCrawlerInterruptedFailure
import org.springframework.stereotype.Component

@Component
internal class DanawaDetailCrawler(
    private val danawaClient: DanawaClient,
    private val summaryFallbackParser: DanawaSummaryFallbackParser,
    private val laptopSnapshotMerger: LaptopSnapshotMerger,
) : ProductDetailCrawler {
    override fun fetchDetailRefreshOutcomes(
        workItems: List<DetailRefreshWorkItem>,
        detailFetchExecutor: DetailFetchExecutor,
    ): List<DetailRefreshOutcome> {
        return detailFetchExecutor.fetch(workItems) { workItem ->
            try {
                DetailRefreshOutcome(
                    workItem = workItem,
                    buildResult = buildLaptop(workItem.productCard),
                )
            } catch (exception: Exception) {
                if (exception.isCrawlerInterruptedFailure()) {
                    throw exception
                }
                DetailRefreshOutcome(
                    workItem = workItem,
                    error = exception,
                )
            }
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

        val summaryFallback = summaryFallbackParser.parseSummaryFallback(
            summaryFallbackParser.extractSummaryText(detailPageHtml),
        )
        if (parsedSpecTable.values.isEmpty() && summaryFallback.isEmpty()) {
            degradationReasons += "상세/요약 스펙 모두 비어 있음"
        }

        return BuildLaptopResult(
            command = laptopSnapshotMerger.createCommand(productCard, parsedSpecTable, summaryFallback),
            degradationReasons = degradationReasons.distinct(),
        )
    }
}
