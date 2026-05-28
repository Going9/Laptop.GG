package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort

internal class LoadExistingCrawledLaptopLookupService(
    private val existingLookupLoader: ExistingCrawledLaptopLookupLoader,
    private val transactionPort: CrawlerTransactionPort,
    private val commandNormalizer: CrawledLaptopCommandNormalizer = CrawledLaptopCommandNormalizer(),
    private val validator: CrawledLaptopCommandValidator = CrawledLaptopCommandValidator(),
) : LoadExistingCrawledLaptopLookupUseCase {
    override fun load(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
        val normalizedProductCards = productCards.map(commandNormalizer::normalizeProductCard)
        normalizedProductCards.forEach(validator::validateProductCard)
        return transactionPort.read {
            existingLookupLoader.load(normalizedProductCards)
        }
    }
}
