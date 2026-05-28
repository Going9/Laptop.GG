package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException

internal class CrawledLaptopCommandValidator {
    fun validateExistingLaptopId(existingLaptopId: Long) {
        if (existingLaptopId <= 0) {
            throw CrawlerInvalidCommandException("existingLaptopId must be positive.")
        }
    }

    fun validateProductCard(productCard: CrawledProductCardCommand) {
        requireNonBlank(fieldName = "productCode", value = productCard.productCode)
        requireNonBlank(fieldName = "productName", value = productCard.productName)
        requireNonBlank(fieldName = "detailPage", value = productCard.detailPage)
        requireNonBlank(fieldName = "imageUrl", value = productCard.imageUrl)
        productCard.price?.let { requireNonNegative(fieldName = "price", value = it) }
    }

    fun validateLaptopCommand(command: CrawledLaptopCommand) {
        requireNonBlank(fieldName = "name", value = command.name)
        requireNonBlank(fieldName = "imageUrl", value = command.imageUrl)
        requireNonBlank(fieldName = "detailPage", value = command.detailPage)
        command.price?.let { requireNonNegative(fieldName = "price", value = it) }
    }

    private fun requireNonBlank(fieldName: String, value: String) {
        if (value.isBlank()) {
            throw CrawlerInvalidCommandException("$fieldName must not be blank.")
        }
    }

    private fun requireNonNegative(fieldName: String, value: Int) {
        if (value < 0) {
            throw CrawlerInvalidCommandException("$fieldName must not be negative.")
        }
    }
}
