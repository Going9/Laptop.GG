package going9.laptopgg.application.crawler.persistence

interface SaveCrawledLaptopUseCase {
    fun loadExistingLookup(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup
    fun saveListSnapshot(existingLaptopId: Long, productCard: CrawledProductCardCommand): SaveResult
    fun saveOrUpdateLaptop(command: CrawledLaptopCommand, existingLaptopId: Long? = null): SaveResult
}
