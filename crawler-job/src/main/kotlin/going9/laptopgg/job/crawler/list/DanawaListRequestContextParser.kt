package going9.laptopgg.job.crawler.list

import going9.laptopgg.job.crawler.source.CrawlSource

internal object DanawaListRequestContextParser {
    fun extractListRequestContext(
        initialListHtml: String,
        crawlSource: CrawlSource,
    ): ListRequestContext {
        val defaults = ListRequestContext()

        return ListRequestContext(
            listUrl = crawlSource.listUrl,
            listCategoryCode = extractJsScalar(initialListHtml, "nListCategoryCode") ?: defaults.listCategoryCode,
            categoryCode = extractJsScalar(initialListHtml, "nCategoryCode") ?: defaults.categoryCode,
            physicsCate1 = extractJsScalar(initialListHtml, "sPhysicsCate1") ?: defaults.physicsCate1,
            physicsCate2 = extractJsScalar(initialListHtml, "sPhysicsCate2") ?: defaults.physicsCate2,
            physicsCate3 = extractJsScalar(initialListHtml, "sPhysicsCate3") ?: defaults.physicsCate3,
            physicsCate4 = extractJsScalar(initialListHtml, "sPhysicsCate4") ?: defaults.physicsCate4,
            viewMethod = extractJsScalar(initialListHtml, "sPriceCompareListType") ?: defaults.viewMethod,
            sortMethod = extractJsScalar(initialListHtml, "sPriceCompareListSort")
                ?: extractJsScalar(initialListHtml, "sProductListSort")
                ?: defaults.sortMethod,
            listCount = extractJsScalar(initialListHtml, "nPriceCompareListCount") ?: defaults.listCount,
            group = extractJsScalar(initialListHtml, "nListGroup") ?: defaults.group,
            depth = extractJsScalar(initialListHtml, "nListDepth") ?: defaults.depth,
            discountProductRate = extractJsScalar(initialListHtml, "sDiscountProductRate") ?: defaults.discountProductRate,
            initialPriceDisplay = extractJsScalar(initialListHtml, "sInitialPriceDisplay") ?: defaults.initialPriceDisplay,
            quickDeliveryCategoryYn = extractJsScalar(initialListHtml, "quickDeliveryCategoryYN") ?: defaults.quickDeliveryCategoryYn,
            quickDeliveryDisplay = extractJsScalar(initialListHtml, "quickDeliveryDisplay") ?: defaults.quickDeliveryDisplay,
            priceUnitSort = extractJsScalar(initialListHtml, "priceUnitSort") ?: defaults.priceUnitSort,
            priceUnitSortOrder = extractJsScalar(initialListHtml, "priceUnitSortOrder") ?: defaults.priceUnitSortOrder,
            simpleDescriptionDisplayYn = extractJsScalar(initialListHtml, "simpleDescriptionDisplayYN")
                ?: defaults.simpleDescriptionDisplayYn,
            simpleDescriptionOpen = extractJsScalar(initialListHtml, "simpleDescriptionOpen") ?: defaults.simpleDescriptionOpen,
            listPackageType = extractJsScalar(initialListHtml, "nPriceCompareListPackageType") ?: defaults.listPackageType,
            priceUnit = extractJsScalar(initialListHtml, "nPriceUnit") ?: defaults.priceUnit,
            priceUnitValue = extractJsScalar(initialListHtml, "nPriceUnitValue") ?: defaults.priceUnitValue,
            priceUnitClass = extractJsScalar(initialListHtml, "sPriceUnitClass") ?: defaults.priceUnitClass,
            cmRecommendSort = extractJsScalar(initialListHtml, "sCmRecommendSort") ?: defaults.cmRecommendSort,
            cmRecommendSortDefault = extractJsScalar(initialListHtml, "sCmRecommendSortDefault")
                ?: defaults.cmRecommendSortDefault,
            bundleImagePreview = extractJsScalar(initialListHtml, "sBundleImagePreview") ?: defaults.bundleImagePreview,
            packageLimit = extractJsScalar(initialListHtml, "nPriceCompareListPackageLimit") ?: defaults.packageLimit,
            makerDisplayYn = extractJsScalar(initialListHtml, "sMakerStandardDisplayStatus")
                ?: extractJsScalar(initialListHtml, "sMakerIndicate")
                ?: defaults.makerDisplayYn,
            dpgZoneUiCategory = extractJsScalar(initialListHtml, "isDpgZoneUICategory") ?: defaults.dpgZoneUiCategory,
            assemblyGalleryCategory = extractJsScalar(initialListHtml, "isAssemblyGalleryCategory")
                ?: defaults.assemblyGalleryCategory,
            searchAttributeValues = crawlSource.attributeFilters.map { it.value },
        )
    }

    private fun extractJsScalar(html: String, key: String): String? {
        val escapedKey = Regex.escape(key)

        Regex("""["']?$escapedKey["']?\s*:\s*"([^"]*)"""")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        return Regex("""["']?$escapedKey["']?\s*:\s*([0-9]+)""")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
