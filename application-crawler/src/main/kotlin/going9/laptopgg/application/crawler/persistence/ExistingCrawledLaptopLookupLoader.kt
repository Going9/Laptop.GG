package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort

internal class ExistingCrawledLaptopLookupLoader(
    private val laptopPort: CrawledLaptopPersistencePort,
) {
    fun load(productCards: List<CrawledProductCardCommand>): ExistingCrawledLaptopLookup {
        if (productCards.isEmpty()) {
            return ExistingCrawledLaptopLookup(emptyMap(), emptyMap())
        }
        validateBatchIdentity(productCards)

        val byProductCode = uniqueLookupMap(
            snapshots = laptopPort.findExistingByProductCodes(productCards.map { it.productCode.trim() }.distinct()),
            identityName = "productCode",
            identityValue = { laptop -> laptop.productCode },
        )
        val byDetailPage = uniqueLookupMap(
            snapshots = laptopPort.findExistingByDetailPages(productCards.map { it.detailPage.trim() }.distinct()),
            identityName = "detailPage",
            identityValue = { laptop -> laptop.detailPage },
        )

        return ExistingCrawledLaptopLookup(
            byProductCode = byProductCode,
            byDetailPage = byDetailPage,
        )
    }

    private fun validateBatchIdentity(productCards: List<CrawledProductCardCommand>) {
        val productCodeConflicts = productCards
            .groupBy { card -> card.productCode.trim() }
            .filterValues { cards -> cards.map { card -> card.detailPage.trim() }.distinct().size > 1 }
        if (productCodeConflicts.isNotEmpty()) {
            throw CrawlerInvalidCommandException(
                "Crawler batch has one productCode mapped to multiple detailPages: " +
                    productCodeConflicts.toConflictSamples { cards -> cards.map { it.detailPage.trim() } },
            )
        }

        val detailPageConflicts = productCards
            .groupBy { card -> card.detailPage.trim() }
            .filterValues { cards -> cards.map { card -> card.productCode.trim() }.distinct().size > 1 }
        if (detailPageConflicts.isNotEmpty()) {
            throw CrawlerInvalidCommandException(
                "Crawler batch has one detailPage mapped to multiple productCodes: " +
                    detailPageConflicts.toConflictSamples { cards -> cards.map { it.productCode.trim() } },
            )
        }
    }

    private fun uniqueLookupMap(
        snapshots: List<ExistingCrawledLaptopSnapshot>,
        identityName: String,
        identityValue: (ExistingCrawledLaptopSnapshot) -> String?,
    ): Map<String, ExistingCrawledLaptopSnapshot> {
        val grouped = snapshots
            .mapNotNull { laptop ->
                identityValue(laptop)
                    ?.trim()
                    ?.takeIf { value -> value.isNotBlank() }
                    ?.let { value -> value to laptop }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        val duplicateGroups = grouped.filterValues { laptops -> laptops.map { it.id }.distinct().size > 1 }
        if (duplicateGroups.isNotEmpty()) {
            throw CrawlerInvalidStateException(
                "Multiple laptops found for $identityName; clean duplicate crawler identities before crawling: " +
                    duplicateGroups.toConflictSamples { laptops -> laptops.map { it.id.toString() } },
            )
        }

        return grouped.mapValues { (_, laptops) -> laptops.first() }
    }

    private fun <T> Map<String, List<T>>.toConflictSamples(values: (List<T>) -> List<String>): String {
        return entries
            .sortedBy { it.key }
            .take(3)
            .joinToString(separator = "; ") { (key, groupedValues) ->
                val sampleValues = values(groupedValues).distinct().sorted().take(5).joinToString(prefix = "[", postfix = "]")
                "$key -> $sampleValues"
            }
    }
}
