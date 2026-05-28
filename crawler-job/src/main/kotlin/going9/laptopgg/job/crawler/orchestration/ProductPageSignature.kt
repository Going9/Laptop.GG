package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.list.ProductCard

internal object ProductPageSignature {
    fun create(productCards: List<ProductCard>): String {
        return productCards.joinToString("||") { it.detailPage }
    }

    fun stableHash(signature: String): String {
        return signature.hashCode().toUInt().toString(16)
    }
}
