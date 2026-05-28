package going9.laptopgg.job.crawler.danawa.list

import going9.laptopgg.job.crawler.danawa.DanawaEndpoints
import going9.laptopgg.job.crawler.list.ListRequestContext

internal object DanawaListRequestDefaults {
    fun context(
        listUrl: String = DanawaEndpoints.NOTEBOOK_LIST_URL,
        searchAttributeValues: List<String> = emptyList(),
    ): ListRequestContext {
        return ListRequestContext(
            listUrl = listUrl,
            listCategoryCode = "758",
            categoryCode = "758",
            physicsCate1 = "860",
            physicsCate2 = "869",
            physicsCate3 = "0",
            physicsCate4 = "0",
            viewMethod = "LIST",
            sortMethod = "SAVEASC",
            listCount = "30",
            group = "11",
            depth = "2",
            discountProductRate = "0",
            initialPriceDisplay = "N",
            mallMinPriceDisplayYn = "Y",
            quickDeliveryCategoryYn = "N",
            quickDeliveryDisplay = "",
            priceUnitSort = "N",
            priceUnitSortOrder = "A",
            simpleDescriptionDisplayYn = "Y",
            simpleDescriptionOpen = "Y",
            listPackageType = "3",
            priceUnit = "0",
            priceUnitValue = "0",
            priceUnitClass = "",
            cmRecommendSort = "N",
            cmRecommendSortDefault = "N",
            bundleImagePreview = "N",
            packageLimit = "7",
            makerDisplayYn = "Y",
            dpgZoneUiCategory = "N",
            assemblyGalleryCategory = "N",
            searchAttributeValues = searchAttributeValues,
        )
    }
}
