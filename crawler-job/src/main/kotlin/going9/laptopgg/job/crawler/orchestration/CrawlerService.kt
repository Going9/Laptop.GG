package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.job.crawler.source.CrawlSourceResolver
import going9.laptopgg.job.config.CrawlerJobProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class CrawlerService(
    private val crawlSourceRunner: CrawlSourceRunner,
    private val crawlSourceResolver: CrawlSourceResolver,
    private val crawlerJobProperties: CrawlerJobProperties,
    private val detailFetchExecutorFactory: DetailFetchExecutorFactory,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun crawlAll(limit: Int? = null, startPage: Int = 1, filterProfileRaw: String? = null): CrawlSummary {
        val resolvedCrawlSources = crawlSourceResolver.resolve(filterProfileRaw)
        val maxListPages = crawlerJobProperties.resolvedMaxListPages()
        val seenDetailPages = linkedSetOf<String>()
        val progress = CrawlProgress()
        var reachedLimit = false
        var hitMaxListPages = false

        detailFetchExecutorFactory.create().use { detailFetchExecutor ->
            val crawlSources = resolvedCrawlSources.sources
            logger.info(
                "크롤링을 시작합니다. filterProfile={}, sourceCount={}, startPage={}, limit={}",
                resolvedCrawlSources.profileName,
                crawlSources.size,
                startPage.coerceAtLeast(1),
                limit ?: "ALL",
            )

            crawlSources.forEachIndexed { index, crawlSource ->
                if (reachedLimit) {
                    return@forEachIndexed
                }

                val sourceResult = crawlSourceRunner.runSource(
                    crawlSource = crawlSource,
                    startPage = if (index == 0) startPage.coerceAtLeast(1) else 1,
                    maxListPages = maxListPages,
                    limit = limit,
                    seenDetailPages = seenDetailPages,
                    progress = progress,
                    detailFetchExecutor = detailFetchExecutor,
                )
                if (sourceResult.reachedLimit) {
                    reachedLimit = true
                }
                if (sourceResult.hitMaxListPages) {
                    hitMaxListPages = true
                }
            }

            if (!reachedLimit && crawlSources.isNotEmpty() && progress.processedCount > 0) {
                logger.info("모든 크롤 소스를 순회했습니다. filterProfile={}", resolvedCrawlSources.profileName)
            }

            if (hitMaxListPages) {
                logger.warn("목록 페이지 안전 제한({})에 도달해 크롤링을 종료합니다.", maxListPages)
            }

            logger.info(
                "크롤링 최종 요약: processedCount={}, createdCount={}, updatedCount={}, detailRefreshCount={}, priceOnlyUpdatedCount={}, degradedCount={}, failedCount={}",
                progress.processedCount,
                progress.createdCount,
                progress.updatedCount,
                progress.detailRefreshCount,
                progress.priceOnlyUpdatedCount,
                progress.degradedCount,
                progress.failedCount,
            )

            return progress.toSummary()
        }
    }
}
