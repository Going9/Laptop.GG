package going9.laptopgg.job.crawler

import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory

@Service
class CrawlerService(
    private val listPageCrawler: ListPageCrawler,
    private val crawlProductBatchProcessor: CrawlProductBatchProcessor,
    private val crawlSourceResolver: CrawlSourceResolver,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun crawlAll(limit: Int? = null, startPage: Int = 1, filterProfileRaw: String? = null): CrawlSummary {
        val filterProfile = crawlSourceResolver.resolveFilterProfile(filterProfileRaw)
        val detailFetchExecutor = Executors.newFixedThreadPool(DETAIL_FETCH_CONCURRENCY)
        val seenDetailPages = linkedSetOf<String>()
        val progress = CrawlProgress()
        var reachedLimit = false
        var hitMaxListPages = false

        try {
            val crawlSources = crawlSourceResolver.resolveCrawlSources(filterProfile)
            logger.info(
                "크롤링을 시작합니다. filterProfile={}, sourceCount={}, startPage={}, limit={}",
                filterProfile.name.lowercase(),
                crawlSources.size,
                startPage.coerceAtLeast(1),
                limit ?: "ALL",
            )

            crawlSources.forEachIndexed { index, crawlSource ->
                if (reachedLimit) {
                    return@forEachIndexed
                }

                val listRequestContext = listPageCrawler.createListRequestContext(crawlSource)
                val requestFilterCount = listRequestContext.searchAttributeValues.size
                val requestDistinctFilterCount = listRequestContext.searchAttributeValues.toSet().size

                run {
                    val seenPageSignatures = linkedSetOf<String>()
                    var currentPage = if (index == 0) startPage.coerceAtLeast(1) else 1
                    var consecutiveDuplicateOnlyPages = 0

                    logger.info(
                        "크롤 소스를 시작합니다. source={}, startPage={}, attributeFilterCount={}, filters={}",
                        crawlSource.key,
                        currentPage,
                        crawlSource.attributeFilters.size,
                        crawlSource.attributeFilters.joinToString { it.name }.ifBlank { "없음" },
                    )

                    while (currentPage <= MAX_LIST_PAGES) {
                        val pageStartTime = System.currentTimeMillis()
                        val pageBatch = listPageCrawler.fetchProductPageBatch(currentPage, listRequestContext)
                        val productCards = pageBatch.productCards
                        val expectedLastPage = pageBatch.priceCompareCount
                            ?.takeIf { it > 0 }
                            ?.let { ((it - 1) / pageBatch.productCards.size.coerceAtLeast(1)) + 1 }

                        if (productCards.isEmpty()) {
                            logger.info("현재 페이지에서 수집 가능한 상품이 없어 크롤링을 종료합니다. source={}, page={}", crawlSource.key, currentPage)
                            break
                        }

                        val pageSignature = DanawaListParser.createPageSignature(productCards)
                        val isRepeatedPageSignature = !seenPageSignatures.add(pageSignature)
                        val freshProductCards = productCards.filter { seenDetailPages.add(it.detailPage) }
                        val duplicateSkippedCount = productCards.size - freshProductCards.size
                        val visiblePagesLog = pageBatch.visiblePageNumbers
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(",")
                            ?: "없음"
                        consecutiveDuplicateOnlyPages = if (freshProductCards.isEmpty()) {
                            consecutiveDuplicateOnlyPages + 1
                        } else {
                            0
                        }

                        val remainingQuota = progress.remainingQuota(limit)
                        if (remainingQuota == 0) {
                            break
                        }

                        val candidateProductCards = remainingQuota?.let(freshProductCards::take) ?: freshProductCards
                        val pageProcessingResult = crawlProductBatchProcessor.process(
                            productCards = candidateProductCards,
                            progress = progress,
                            detailFetchExecutor = detailFetchExecutor,
                        )

                        if (progress.reachedLimit(limit)) {
                            reachedLimit = true
                        }

                        if (currentPage == 1 || freshProductCards.isEmpty() || isRepeatedPageSignature) {
                            logger.info(
                                "페이지 진단: source={}, page={}, hasNextPage={}, priceCompareCount={}, expectedLastPage={}, visiblePages={}, nextPageHint={}, repeatedPageSignature={}, pageSignatureHash={}, firstCard={}, lastCard={}, requestPage={}, requestSortMethod={}, requestFilterCount={}, requestDistinctFilterCount={}",
                                crawlSource.key,
                                currentPage,
                                pageBatch.hasNextPage,
                                pageBatch.priceCompareCount ?: "알 수 없음",
                                expectedLastPage ?: "알 수 없음",
                                visiblePagesLog,
                                pageBatch.nextPageHint ?: "없음",
                                isRepeatedPageSignature,
                                pageSignature.stableHash(),
                                describeCard(productCards.firstOrNull()),
                                describeCard(productCards.lastOrNull()),
                                currentPage,
                                listRequestContext.sortMethod,
                                requestFilterCount,
                                requestDistinctFilterCount,
                            )
                        }

                        logger.info(
                            "페이지 처리 시간: ${System.currentTimeMillis() - pageStartTime}ms / source=${crawlSource.key} / page=${currentPage} / " +
                                "수집 상품: ${productCards.size}개 / 신규 상품: ${freshProductCards.size}개 / 실제 처리: ${pageProcessingResult.processedCount}개 / " +
                                "상세 재수집: ${pageProcessingResult.detailRefreshCount}개 / 중복 스킵: ${duplicateSkippedCount}개 / " +
                                "가격만 갱신(페이지): ${pageProcessingResult.pagePriceOnlyUpdatedCount}개 / 가격만 갱신(누적): ${progress.priceOnlyUpdatedCount}개 / " +
                                "누적 처리: ${progress.processedCount}개 / 누적 열화: ${progress.degradedCount}개 / 누적 실패: ${progress.failedCount}개",
                        )

                        if (reachedLimit) {
                            break
                        }

                        if (expectedLastPage != null && currentPage >= expectedLastPage) {
                            logger.info(
                                "총 상품 수 기준 마지막 페이지에 도달해 크롤링을 종료합니다. source={}, page={}, priceCompareCount={}, expectedLastPage={}, hasNextPage={}",
                                crawlSource.key,
                                currentPage,
                                pageBatch.priceCompareCount,
                                expectedLastPage,
                                pageBatch.hasNextPage,
                            )
                            break
                        }

                        if (shouldStopAtDuplicateTail(
                                freshProductCount = freshProductCards.size,
                                consecutiveDuplicateOnlyPages = consecutiveDuplicateOnlyPages,
                            )
                        ) {
                            logger.info(
                                "AJAX 페이지네이션에서도 새 detail 페이지가 없는 반복 목록이 이어져 크롤링을 종료합니다. source={}, page={}, repeatedPageSignature={}, consecutiveDuplicateOnlyPages={}, hasNextPage={}, visiblePages={}, nextPageHint={}, priceCompareCount={}, expectedLastPage={}, pageSignatureHash={}, firstCard={}, lastCard={}, requestPage={}, requestSortMethod={}, requestFilterCount={}, requestDistinctFilterCount={}",
                                crawlSource.key,
                                currentPage,
                                isRepeatedPageSignature,
                                consecutiveDuplicateOnlyPages,
                                pageBatch.hasNextPage,
                                visiblePagesLog,
                                pageBatch.nextPageHint ?: "없음",
                                pageBatch.priceCompareCount ?: "알 수 없음",
                                expectedLastPage ?: "알 수 없음",
                                pageSignature.stableHash(),
                                describeCard(productCards.firstOrNull()),
                                describeCard(productCards.lastOrNull()),
                                currentPage,
                                listRequestContext.sortMethod,
                                requestFilterCount,
                                requestDistinctFilterCount,
                            )
                            break
                        }

                        if (!pageBatch.hasNextPage) {
                            logger.info("다음 페이지가 없어 크롤링을 종료합니다. source={}, page={}", crawlSource.key, currentPage)
                            break
                        }

                        currentPage++
                    }

                    if (currentPage > MAX_LIST_PAGES) {
                        hitMaxListPages = true
                    }
                }
            }

            if (!reachedLimit && crawlSources.isNotEmpty() && progress.processedCount > 0) {
                logger.info("모든 크롤 소스를 순회했습니다. filterProfile={}", filterProfile.name.lowercase())
            }

            if (hitMaxListPages) {
                logger.warn("목록 페이지 안전 제한({})에 도달해 크롤링을 종료합니다.", MAX_LIST_PAGES)
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
        } finally {
            detailFetchExecutor.shutdown()
        }
    }

    internal fun shouldStopAtDuplicateTail(
        freshProductCount: Int,
        consecutiveDuplicateOnlyPages: Int,
    ): Boolean {
        if (freshProductCount > 0) {
            return false
        }

        return consecutiveDuplicateOnlyPages >= MAX_CONSECUTIVE_DUPLICATE_ONLY_PAGES
    }

    private fun describeCard(productCard: ProductCard?): String {
        if (productCard == null) {
            return "없음"
        }

        val cate = DanawaListParser.extractQueryParam(productCard.detailPage, "cate") ?: productCard.cate4
        return "${productCard.productCode}@${cate}"
    }

    private fun String.stableHash(): String {
        return hashCode().toUInt().toString(16)
    }

    companion object {
        private const val MAX_LIST_PAGES = 5000
        private const val MAX_CONSECUTIVE_DUPLICATE_ONLY_PAGES = 5
        private const val DETAIL_FETCH_CONCURRENCY = 6
    }
}
