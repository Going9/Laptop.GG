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
) {
    fun toFormData(page: Int): List<Pair<String, String?>> {
        return buildList {
            add("page" to page.toString())
            add("listCategoryCode" to listCategoryCode)
            add("categoryCode" to categoryCode)
            add("physicsCate1" to physicsCate1)
            add("physicsCate2" to physicsCate2)
            add("physicsCate3" to physicsCate3)
            add("physicsCate4" to physicsCate4)
            add("viewMethod" to viewMethod)
            add("sortMethod" to sortMethod)
            add("listCount" to listCount)
            add("group" to group)
            add("depth" to depth)
            add("brandName" to "")
            add("makerName" to "")
            add("searchOptionName" to "")
            searchAttributeValues.forEach { add("searchAttributeValue[]" to it) }
            add("sDiscountProductRate" to discountProductRate)
            add("sInitialPriceDisplay" to initialPriceDisplay)
            add("sPowerLinkKeyword" to "")
            add("oCurrentCategoryCode" to "")
            add("sMallMinPriceDisplayYN" to mallMinPriceDisplayYn)
            add("quickDeliveryCategoryYN" to quickDeliveryCategoryYn)
            add("quickDeliveryDisplay" to quickDeliveryDisplay)
            add("priceUnitSort" to priceUnitSort)
            add("priceUnitSortOrder" to priceUnitSortOrder)
            add("simpleDescriptionDisplayYN" to simpleDescriptionDisplayYn)
            add("simpleDescriptionOpen" to simpleDescriptionOpen)
            add("listPackageType" to listPackageType)
            add("categoryMappingCode" to "")
            add("priceUnit" to priceUnit)
            add("priceUnitValue" to priceUnitValue)
            add("priceUnitClass" to priceUnitClass)
            add("cmRecommendSort" to cmRecommendSort)
            add("cmRecommendSortDefault" to cmRecommendSortDefault)
            add("bundleImagePreview" to bundleImagePreview)
            add("nPackageLimit" to packageLimit)
            add("bMakerDisplayYN" to makerDisplayYn)
            add("dnwSwitchOn" to "")
            add("isDpgZoneUICategory" to dpgZoneUiCategory)
            add("isAssemblyGalleryCategory" to assemblyGalleryCategory)
        }
    }
}

internal data class ProductPageBatch(
    val productCards: List<ProductCard>,
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
