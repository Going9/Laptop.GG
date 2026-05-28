package going9.laptopgg.job.crawler.danawa.client

import going9.laptopgg.job.crawler.list.ListRequestContext

internal object DanawaListRequestFormData {
    fun from(context: ListRequestContext, page: Int): List<Pair<String, String?>> {
        return buildList {
            add("page" to page.toString())
            add("listCategoryCode" to context.listCategoryCode)
            add("categoryCode" to context.categoryCode)
            add("physicsCate1" to context.physicsCate1)
            add("physicsCate2" to context.physicsCate2)
            add("physicsCate3" to context.physicsCate3)
            add("physicsCate4" to context.physicsCate4)
            add("viewMethod" to context.viewMethod)
            add("sortMethod" to context.sortMethod)
            add("listCount" to context.listCount)
            add("group" to context.group)
            add("depth" to context.depth)
            add("brandName" to "")
            add("makerName" to "")
            add("searchOptionName" to "")
            context.searchAttributeValues.forEach { add("searchAttributeValue[]" to it) }
            add("sDiscountProductRate" to context.discountProductRate)
            add("sInitialPriceDisplay" to context.initialPriceDisplay)
            add("sPowerLinkKeyword" to "")
            add("oCurrentCategoryCode" to "")
            add("sMallMinPriceDisplayYN" to context.mallMinPriceDisplayYn)
            add("quickDeliveryCategoryYN" to context.quickDeliveryCategoryYn)
            add("quickDeliveryDisplay" to context.quickDeliveryDisplay)
            add("priceUnitSort" to context.priceUnitSort)
            add("priceUnitSortOrder" to context.priceUnitSortOrder)
            add("simpleDescriptionDisplayYN" to context.simpleDescriptionDisplayYn)
            add("simpleDescriptionOpen" to context.simpleDescriptionOpen)
            add("listPackageType" to context.listPackageType)
            add("categoryMappingCode" to "")
            add("priceUnit" to context.priceUnit)
            add("priceUnitValue" to context.priceUnitValue)
            add("priceUnitClass" to context.priceUnitClass)
            add("cmRecommendSort" to context.cmRecommendSort)
            add("cmRecommendSortDefault" to context.cmRecommendSortDefault)
            add("bundleImagePreview" to context.bundleImagePreview)
            add("nPackageLimit" to context.packageLimit)
            add("bMakerDisplayYN" to context.makerDisplayYn)
            add("dnwSwitchOn" to "")
            add("isDpgZoneUICategory" to context.dpgZoneUiCategory)
            add("isAssemblyGalleryCategory" to context.assemblyGalleryCategory)
        }
    }
}
