package going9.laptopgg.application.crawler.persistence

data class ExistingCrawledLaptopLookup(
    val byProductCode: Map<String, ExistingCrawledLaptopSnapshot>,
    val byDetailPage: Map<String, ExistingCrawledLaptopSnapshot>,
) {
    fun find(productCard: CrawledProductCardCommand): ExistingCrawledLaptopSnapshot? {
        return byProductCode[productCard.productCode]
            ?: byDetailPage[productCard.detailPage]
    }
}
