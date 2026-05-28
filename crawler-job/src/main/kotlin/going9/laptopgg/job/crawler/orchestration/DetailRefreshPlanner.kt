package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopLookup
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.job.crawler.detail.DetailRefreshPolicy
import going9.laptopgg.job.crawler.detail.DetailRefreshWorkItem
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.toCommand

internal object DetailRefreshPlanner {
    fun plan(
        productCards: List<ProductCard>,
        existingLookup: ExistingCrawledLaptopLookup,
    ): DetailRefreshWorkPlan {
        val priceOnlySnapshotWorkItems = mutableListOf<PriceOnlySnapshotWorkItem>()
        val detailRefreshWorkItems = mutableListOf<DetailRefreshWorkItem>()

        for (productCard in productCards) {
            val existingLaptop = existingLookup.find(productCard.toCommand())
            if (existingLaptop != null && !DetailRefreshPolicy.needsRefresh(existingLaptop)) {
                priceOnlySnapshotWorkItems += PriceOnlySnapshotWorkItem(
                    productCard = productCard,
                    existingLaptop = existingLaptop,
                )
            } else {
                detailRefreshWorkItems += DetailRefreshWorkItem(
                    productCard = productCard,
                    existingLaptop = existingLaptop,
                )
            }
        }

        return DetailRefreshWorkPlan(
            priceOnlySnapshotWorkItems = priceOnlySnapshotWorkItems,
            detailRefreshWorkItems = detailRefreshWorkItems,
        )
    }
}

internal data class DetailRefreshWorkPlan(
    val priceOnlySnapshotWorkItems: List<PriceOnlySnapshotWorkItem>,
    val detailRefreshWorkItems: List<DetailRefreshWorkItem>,
)

internal data class PriceOnlySnapshotWorkItem(
    val productCard: ProductCard,
    val existingLaptop: ExistingCrawledLaptopSnapshot,
)
