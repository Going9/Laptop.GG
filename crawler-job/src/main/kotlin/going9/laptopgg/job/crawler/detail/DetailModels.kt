package going9.laptopgg.job.crawler.detail

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.job.crawler.list.ProductCard

internal data class BuildLaptopResult(
    val command: CrawledLaptopCommand,
    val degradationReasons: List<String>,
) {
    val isDegraded: Boolean
        get() = degradationReasons.isNotEmpty()
}

internal data class DetailRefreshWorkItem(
    val productCard: ProductCard,
    val existingLaptop: ExistingCrawledLaptopSnapshot?,
)

internal data class DetailRefreshOutcome(
    val workItem: DetailRefreshWorkItem,
    val buildResult: BuildLaptopResult? = null,
    val error: Exception? = null,
)
