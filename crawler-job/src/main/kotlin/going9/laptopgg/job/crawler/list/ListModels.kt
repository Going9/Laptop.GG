package going9.laptopgg.job.crawler.list

import going9.laptopgg.application.crawler.persistence.CrawledProductCardCommand

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
    val listUrl: String,
    val listCategoryCode: String,
    val categoryCode: String,
    val physicsCate1: String,
    val physicsCate2: String,
    val physicsCate3: String,
    val physicsCate4: String,
    val viewMethod: String,
    val sortMethod: String,
    val listCount: String,
    val group: String,
    val depth: String,
    val discountProductRate: String,
    val initialPriceDisplay: String,
    val mallMinPriceDisplayYn: String,
    val quickDeliveryCategoryYn: String,
    val quickDeliveryDisplay: String,
    val priceUnitSort: String,
    val priceUnitSortOrder: String,
    val simpleDescriptionDisplayYn: String,
    val simpleDescriptionOpen: String,
    val listPackageType: String,
    val priceUnit: String,
    val priceUnitValue: String,
    val priceUnitClass: String,
    val cmRecommendSort: String,
    val cmRecommendSortDefault: String,
    val bundleImagePreview: String,
    val packageLimit: String,
    val makerDisplayYn: String,
    val dpgZoneUiCategory: String,
    val assemblyGalleryCategory: String,
    val searchAttributeValues: List<String>,
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
