package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard
import org.springframework.stereotype.Component

@Component
class CrawlPageDiagnosticFormatter {
    internal fun format(context: CrawlPageDiagnosticContext): FormattedCrawlPageDiagnostics {
        return FormattedCrawlPageDiagnostics(
            visiblePages = visiblePages(context),
            nextPageHint = context.pageBatch.nextPageHint?.toString() ?: "없음",
            priceCompareCount = context.pageBatch.priceCompareCount?.toString() ?: "알 수 없음",
            expectedLastPage = context.expectedLastPage?.toString() ?: "알 수 없음",
            pageSignatureHash = ProductPageSignature.stableHash(context.pageSignature),
            firstCard = describeCard(context.productCards.firstOrNull()),
            lastCard = describeCard(context.productCards.lastOrNull()),
        )
    }

    private fun visiblePages(context: CrawlPageDiagnosticContext): String {
        return context.pageBatch.visiblePageNumbers
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
