package going9.laptopgg.job.crawler.danawa.client

import going9.laptopgg.job.crawler.danawa.detail.DetailRequestContext
import going9.laptopgg.job.crawler.list.ListRequestContext
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.support.isCrawlerInterruptedFailure
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class DanawaClient(
    private val danawaHttpClient: DanawaHttpClient,
    private val requestFactory: DanawaRequestFactory,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    internal fun fetchInitialListPage(listUrl: String): String {
        return danawaHttpClient.send(requestFactory.initialListPage(listUrl))
    }

    internal fun fetchListPage(page: Int, listRequestContext: ListRequestContext): String {
        return danawaHttpClient.send(requestFactory.listPage(page, listRequestContext))
    }

    internal fun fetchDetailPage(detailPage: String): String {
        return danawaHttpClient.send(requestFactory.detailPage(detailPage))
    }

    internal fun fetchDetailSpec(
        productCard: ProductCard,
        detailRequestContext: DetailRequestContext?,
    ): String? {
        val request = requestFactory.detailSpec(productCard, detailRequestContext) ?: return null

        val responseBody = try {
            danawaHttpClient.send(request)
        } catch (e: DanawaHttpException) {
            if (e.isCrawlerInterruptedFailure()) {
                throw e
            }
            logger.warn("상세 스펙 테이블 요청 실패, 요약 스펙으로 대체합니다: {} / {}", productCard.detailPage, e.message)
            return null
        }

        return responseBody.takeIf { it.contains("spec_tbl") }
    }
}
