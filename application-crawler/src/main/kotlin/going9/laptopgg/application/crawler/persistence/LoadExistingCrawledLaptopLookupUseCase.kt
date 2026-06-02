package going9.laptopgg.application.crawler.persistence

interface LoadExistingCrawledLaptopLookupUseCase {
    fun load(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup
}
