package going9.laptopgg.application.crawler.persistence

internal class CrawledLaptopCommandNormalizer(
    private val fieldChangePolicy: CrawledLaptopFieldChangePolicy = CrawledLaptopFieldChangePolicy(),
) {
    fun normalizeProductCard(productCard: CrawledProductCardCommand): CrawledProductCardCommand {
        return productCard.copy(
            productCode = fieldChangePolicy.normalizeRequiredText(productCard.productCode),
            productName = fieldChangePolicy.normalizeRequiredText(productCard.productName),
            detailPage = fieldChangePolicy.normalizeRequiredText(productCard.detailPage),
            imageUrl = fieldChangePolicy.normalizeRequiredText(productCard.imageUrl),
        )
    }

    fun normalizeDetailCommand(command: CrawledLaptopCommand): CrawledLaptopCommand {
        return command.copy(
            name = fieldChangePolicy.normalizeRequiredText(command.name),
            imageUrl = fieldChangePolicy.normalizeRequiredText(command.imageUrl),
            detailPage = fieldChangePolicy.normalizeRequiredText(command.detailPage),
            productCode = fieldChangePolicy.normalizeOptionalText(command.productCode),
            cpuManufacturer = fieldChangePolicy.normalizeOptionalText(command.cpuManufacturer),
            cpu = fieldChangePolicy.normalizeOptionalText(command.cpu),
            os = fieldChangePolicy.normalizeOptionalText(command.os),
            resolution = fieldChangePolicy.normalizeOptionalText(command.resolution),
            ramType = fieldChangePolicy.normalizeOptionalText(command.ramType),
            graphicsType = fieldChangePolicy.normalizeOptionalText(command.graphicsType),
            sdCard = fieldChangePolicy.normalizeOptionalText(command.sdCard),
            usages = fieldChangePolicy.normalizeUsages(command.usages),
        )
    }
}
