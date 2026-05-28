package going9.laptopgg.job.crawler.list

import going9.laptopgg.application.crawler.persistence.CrawledProductCardCommand
import going9.laptopgg.job.crawler.source.CrawlerUrls

internal data class ProductCard(
    val productCode: String,
    val productName: String,
    val detailPage: String,
    val imageUrl: String,
    val price: Int?,
    val cate1: String,
    val cate2: String,
    val cate3: String,
    val cate4: String,
)

internal data class ListRequestContext(
    val listUrl: String = CrawlerUrls.NOTEBOOK_LIST_URL,
    val listCategoryCode: String = "758",
    val categoryCode: String = "758",
    val physicsCate1: String = "860",
    val physicsCate2: String = "869",
    val physicsCate3: String = "0",
    val physicsCate4: String = "0",
    val viewMethod: String = "LIST",
    val sortMethod: String = "SAVEASC",
    val listCount: String = "30",
    val group: String = "11",
    val depth: String = "2",
    val discountProductRate: String = "0",
    val initialPriceDisplay: String = "N",
    val mallMinPriceDisplayYn: String = "Y",
    val quickDeliveryCategoryYn: String = "N",
    val quickDeliveryDisplay: String = "",
    val priceUnitSort: String = "N",
    val priceUnitSortOrder: String = "A",
    val simpleDescriptionDisplayYn: String = "Y",
    val simpleDescriptionOpen: String = "Y",
    val listPackageType: String = "3",
    val priceUnit: String = "0",
    val priceUnitValue: String = "0",
    val priceUnitClass: String = "",
    val cmRecommendSort: String = "N",
    val cmRecommendSortDefault: String = "N",
    val bundleImagePreview: String = "N",
    val packageLimit: String = "7",
    val makerDisplayYn: String = "Y",
    val dpgZoneUiCategory: String = "N",
    val assemblyGalleryCategory: String = "N",
    val searchAttributeValues: List<String> = emptyList(),
)

internal data class ProductPageBatch(
    val productCards: List<ProductCard>,
    val hasNextPage: Boolean,
    val priceCompareCount: Int?,
    val visiblePageNumbers: List<Int>,
    val nextPageHint: Int?,
)

internal data class ListPageMetadata(
    val hasNextPage: Boolean,
    val priceCompareCount: Int?,
    val visiblePageNumbers: List<Int>,
    val nextPageHint: Int?,
)

internal fun ProductCard.toCommand(): CrawledProductCardCommand {
    return CrawledProductCardCommand(
        productCode = productCode,
        productName = productName,
        detailPage = detailPage,
        imageUrl = imageUrl,
        price = price,
    )
}
