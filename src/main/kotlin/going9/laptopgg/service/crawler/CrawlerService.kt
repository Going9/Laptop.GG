package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopProfileFactory
import going9.laptopgg.service.LaptopPriceHistoryService
import going9.laptopgg.service.LaptopProfileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import org.slf4j.LoggerFactory

@Service
class CrawlerService(
    private val laptopRepository: LaptopRepository,
    private val laptopProfileService: LaptopProfileService,
    private val laptopProfileFactory: LaptopProfileFactory,
    private val laptopPriceHistoryService: LaptopPriceHistoryService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val requestPacingLock = Any()
    @Volatile
    private var nextAllowedRequestAtMillis = 0L
    @Volatile
    private var globalCooldownUntilMillis = 0L

    data class CrawlSummary(
        val processedCount: Int,
        val createdCount: Int,
        val updatedCount: Int,
        val degradedCount: Int,
        val degradedSamples: List<String>,
        val failedCount: Int,
        val failureSamples: List<String>,
    )

    internal data class BuildLaptopResult(
        val laptop: Laptop,
        val degradationReasons: List<String>,
    ) {
        val isDegraded: Boolean
            get() = degradationReasons.isNotEmpty()
    }

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

    internal data class DetailRequestContext(
        val makerName: String?,
        val productName: String?,
        val prodType: String?,
    )

    internal data class ParsedSpecTable(
        val values: Map<String, String>,
        val usages: List<String>,
    )

    internal data class SummaryFallback(
        val cpuManufacturer: String? = null,
        val cpu: String? = null,
        val os: String? = null,
        val screenSize: Int? = null,
        val resolution: String? = null,
        val brightness: Int? = null,
        val refreshRate: Int? = null,
        val ramSize: Int? = null,
        val ramType: String? = null,
        val isRamReplaceable: Boolean? = null,
        val graphicsKind: String? = null,
        val graphicsModel: String? = null,
        val tgp: Int? = null,
        val isSupportsPdCharging: Boolean? = null,
        val batteryCapacity: Double? = null,
        val storageCapacity: Int? = null,
        val storageSlotCount: Int? = null,
        val weight: Double? = null,
        val usages: List<String> = emptyList(),
    )

    internal data class ListRequestContext(
        val listUrl: String = NOTEBOOK_LIST_URL,
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

    internal data class CrawlSource(
        val key: String,
        val listUrl: String,
        val attributeFilters: List<CrawlerAttributeFilter> = emptyList(),
    )

    internal data class ExistingLookup(
        val byProductCode: Map<String, Laptop>,
        val byDetailPage: Map<String, Laptop>,
    )

    internal data class DetailRefreshWorkItem(
        val productCard: ProductCard,
        val existingLaptop: Laptop?,
    )

    internal data class DetailRefreshOutcome(
        val workItem: DetailRefreshWorkItem,
        val buildResult: BuildLaptopResult? = null,
        val error: Exception? = null,
    )

    internal data class ProductPageBatch(
        val productCards: List<ProductCard>,
        val hasNextPage: Boolean,
        val priceCompareCount: Int?,
        val visiblePageNumbers: List<Int>,
        val nextPageHint: Int?,
    )

    internal enum class SaveResult {
        CREATED,
        UPDATED,
        UNCHANGED,
    }

    internal enum class FilterProfile {
        NONE,
        CORE,
        EXTENDED,
    }

    fun crawlAll(limit: Int? = null, startPage: Int = 1, filterProfileRaw: String? = null): CrawlSummary {
        val filterProfile = resolveFilterProfile(filterProfileRaw)
        val detailFetchExecutor = Executors.newFixedThreadPool(DETAIL_FETCH_CONCURRENCY)
        val seenDetailPages = linkedSetOf<String>()
        var processedCount = 0
        var createdCount = 0
        var updatedCount = 0
        var degradedCount = 0
        var priceOnlyUpdatedCount = 0
        var detailRefreshCount = 0
        val degradedSamples = mutableListOf<String>()
        var failedCount = 0
        val failureSamples = mutableListOf<String>()
        var reachedLimit = false
        var hitMaxListPages = false

        try {
            val crawlSources = resolveCrawlSources(filterProfile)
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

                val listRequestContext = createListRequestContext(crawlSource)
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
                        val pageBatch = buildProductPageBatch(
                            page = currentPage,
                            html = fetchListPageHtml(currentPage, listRequestContext),
                        )
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

                        val remainingQuota = limit?.let { (it - processedCount).coerceAtLeast(0) }
                        if (remainingQuota == 0) {
                            break
                        }

                        val candidateProductCards = remainingQuota?.let(freshProductCards::take) ?: freshProductCards
                        processedCount += candidateProductCards.size
                        var pagePriceOnlyUpdatedCount = 0

                        val existingLookup = loadExistingLookup(candidateProductCards)
                        val detailRefreshWorkItems = mutableListOf<DetailRefreshWorkItem>()

                        for (productCard in candidateProductCards) {
                            val existingLaptop = findExistingLaptop(productCard, existingLookup)
                            if (existingLaptop != null && !DetailRefreshPolicy.needsRefresh(existingLaptop)) {
                                try {
                                    when (saveListSnapshot(existingLaptop, productCard)) {
                                        SaveResult.UPDATED -> {
                                            updatedCount++
                                            priceOnlyUpdatedCount++
                                            pagePriceOnlyUpdatedCount++
                                        }
                                        SaveResult.UNCHANGED -> Unit
                                        SaveResult.CREATED -> Unit
                                    }
                                } catch (e: Exception) {
                                    failedCount++
                                    recordSample(
                                        samples = failureSamples,
                                        productCard = productCard,
                                        reason = e.message ?: e::class.simpleName ?: "알 수 없는 오류",
                                    )
                                    logger.error(
                                        "기존 상품 가격/목록 스냅샷 업데이트 중 오류 발생. productCode={}, detailPage={}",
                                        productCard.productCode,
                                        productCard.detailPage,
                                        e,
                                    )
                                }
                            } else {
                                detailRefreshWorkItems += DetailRefreshWorkItem(
                                    productCard = productCard,
                                    existingLaptop = existingLaptop,
                                )
                            }
                        }

                        detailRefreshCount += detailRefreshWorkItems.size
                        val detailRefreshOutcomes = fetchDetailRefreshOutcomes(detailRefreshWorkItems, detailFetchExecutor)

                        for (detailRefreshOutcome in detailRefreshOutcomes) {
                            val productCard = detailRefreshOutcome.workItem.productCard
                            val existingLaptop = detailRefreshOutcome.workItem.existingLaptop
                            try {
                                val buildResult = detailRefreshOutcome.buildResult
                                if (buildResult != null) {
                                    if (buildResult.isDegraded) {
                                        degradedCount++
                                        recordSample(
                                            samples = degradedSamples,
                                            productCard = productCard,
                                            reason = buildResult.degradationReasons.joinToString(" | "),
                                        )
                                        logger.warn(
                                            "상품 일부 스펙을 요약/기존값으로 보완했습니다. productCode={}, detailPage={}, reasons={}",
                                            productCard.productCode,
                                            productCard.detailPage,
                                            buildResult.degradationReasons,
                                        )
                                    }

                                    when (saveOrUpdateLaptop(buildResult.laptop, existingLaptop)) {
                                        SaveResult.CREATED -> createdCount++
                                        SaveResult.UPDATED -> updatedCount++
                                        SaveResult.UNCHANGED -> Unit
                                    }
                                    continue
                                }

                                if (existingLaptop != null) {
                                    when (saveListSnapshot(existingLaptop, productCard)) {
                                        SaveResult.UPDATED -> {
                                            updatedCount++
                                            priceOnlyUpdatedCount++
                                            pagePriceOnlyUpdatedCount++
                                        }
                                        SaveResult.UNCHANGED -> Unit
                                        SaveResult.CREATED -> Unit
                                    }
                                }

                                val error = detailRefreshOutcome.error
                                failedCount++
                                recordSample(
                                    samples = failureSamples,
                                    productCard = productCard,
                                    reason = error?.message ?: error?.javaClass?.simpleName ?: "알 수 없는 오류",
                                )
                                logger.error(
                                    "상품 상세 재수집 중 오류 발생. productCode={}, detailPage={}",
                                    productCard.productCode,
                                    productCard.detailPage,
                                    error,
                                )
                            } catch (e: Exception) {
                                failedCount++
                                recordSample(
                                    samples = failureSamples,
                                    productCard = productCard,
                                    reason = e.message ?: e::class.simpleName ?: "알 수 없는 오류",
                                )
                                logger.error("상품 크롤링 저장 중 오류 발생. productCode={}, detailPage={}", productCard.productCode, productCard.detailPage, e)
                            }
                        }

                        if (limit != null && processedCount >= limit) {
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
                                "수집 상품: ${productCards.size}개 / 신규 상품: ${freshProductCards.size}개 / 실제 처리: ${candidateProductCards.size}개 / " +
                                "상세 재수집: ${detailRefreshWorkItems.size}개 / 중복 스킵: ${duplicateSkippedCount}개 / " +
                                "가격만 갱신(페이지): ${pagePriceOnlyUpdatedCount}개 / 가격만 갱신(누적): ${priceOnlyUpdatedCount}개 / " +
                                "누적 처리: ${processedCount}개 / 누적 열화: ${degradedCount}개 / 누적 실패: ${failedCount}개",
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

            if (!reachedLimit && crawlSources.isNotEmpty() && processedCount > 0) {
                logger.info("모든 크롤 소스를 순회했습니다. filterProfile={}", filterProfile.name.lowercase())
            }

            if (hitMaxListPages) {
                logger.warn("목록 페이지 안전 제한({})에 도달해 크롤링을 종료합니다.", MAX_LIST_PAGES)
            }

            logger.info(
                "크롤링 최종 요약: processedCount={}, createdCount={}, updatedCount={}, detailRefreshCount={}, priceOnlyUpdatedCount={}, degradedCount={}, failedCount={}",
                processedCount,
                createdCount,
                updatedCount,
                detailRefreshCount,
                priceOnlyUpdatedCount,
                degradedCount,
                failedCount,
            )

            return CrawlSummary(
                processedCount = processedCount,
                createdCount = createdCount,
                updatedCount = updatedCount,
                degradedCount = degradedCount,
                degradedSamples = degradedSamples.toList(),
                failedCount = failedCount,
                failureSamples = failureSamples.toList(),
            )
        } finally {
            detailFetchExecutor.shutdown()
        }
    }

    private fun createListRequestContext(crawlSource: CrawlSource): ListRequestContext {
        val initialListHtml = fetchListPageHtml(crawlSource.listUrl)
        return DanawaListParser.extractListRequestContext(initialListHtml, crawlSource)
            .copy(sortMethod = LIST_SORT_METHOD)
    }

    private fun fetchListPageHtml(listUrl: String): String {
        val request = HttpRequest.newBuilder(URI.create(listUrl))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()

        return sendRequest(request)
    }

    private fun fetchListPageHtml(page: Int, listRequestContext: ListRequestContext): String {
        val request = HttpRequest.newBuilder(URI.create(LIST_AJAX_URL))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", FORM_URLENCODED)
            .header("Origin", DANAWA_ORIGIN)
            .header("Referer", listRequestContext.listUrl)
            .header("X-Requested-With", "XMLHttpRequest")
            .POST(HttpRequest.BodyPublishers.ofString(buildFormData(listRequestContext.toFormData(page))))
            .build()

        return sendRequest(request)
    }

    private fun buildProductPageBatch(
        page: Int,
        html: String,
    ): ProductPageBatch {
        return ProductPageBatch(
            productCards = DanawaListParser.parseListPage(html),
            hasNextPage = DanawaListParser.hasNextPage(html, page),
            priceCompareCount = DanawaListParser.extractPriceCompareCount(html),
            visiblePageNumbers = DanawaListParser.extractVisiblePageNumbers(html),
            nextPageHint = DanawaListParser.extractNextPageHint(html),
        )
    }

    private fun buildLaptop(productCard: ProductCard): BuildLaptopResult {
        val degradationReasons = mutableListOf<String>()
        val detailPageHtml = fetchDetailPageHtml(productCard.detailPage)
        val detailContext = DanawaDetailParser.extractDetailRequestContext(detailPageHtml)
        if (detailContext == null) {
            degradationReasons += "상세 스펙 요청 컨텍스트 없음"
        }
        val detailSpecHtml = fetchDetailSpecHtml(productCard, detailContext)
        if (detailSpecHtml == null) {
            degradationReasons += "상세 스펙 테이블 미수집"
        }
        val parsedSpecTable = detailSpecHtml?.let(DanawaDetailParser::parseSpecTable) ?: ParsedSpecTable(emptyMap(), emptyList())
        if (detailSpecHtml != null && parsedSpecTable.values.isEmpty()) {
            degradationReasons += "상세 스펙 테이블 파싱 결과 비어 있음"
        }
        val summaryFallback = DanawaDetailParser.parseSummaryFallback(DanawaDetailParser.extractSummaryText(detailPageHtml))
        if (parsedSpecTable.values.isEmpty() && DanawaDetailParser.isEmpty(summaryFallback)) {
            degradationReasons += "상세/요약 스펙 모두 비어 있음"
        }

        return BuildLaptopResult(
            laptop = createLaptop(productCard, parsedSpecTable, summaryFallback),
            degradationReasons = degradationReasons.distinct(),
        )
    }

    private fun createLaptop(
        productCard: ProductCard,
        parsedSpecTable: ParsedSpecTable,
        summaryFallback: SummaryFallback,
    ): Laptop {
        val spec = parsedSpecTable.values
        val rawCpu = spec["CPU 넘버"]?.substringBefore(" (") ?: summaryFallback.cpu
        val cpuManufacturer = resolveCpuManufacturer(
            rawManufacturer = spec["CPU 제조사"] ?: summaryFallback.cpuManufacturer,
            productName = productCard.productName,
            rawCpu = rawCpu,
        )
        val cpu = resolveCpuModel(
            rawCpu = rawCpu,
            cpuManufacturer = cpuManufacturer,
            productName = productCard.productName,
        )
        val gpuKind = spec["GPU 종류"] ?: summaryFallback.graphicsKind
        val gpuModel = spec["GPU 칩셋"] ?: summaryFallback.graphicsModel ?: gpuKind
        val usages = parsedSpecTable.usages.ifEmpty { summaryFallback.usages }

        val laptop = Laptop(
            name = productCard.productName,
            imageUrl = productCard.imageUrl,
            detailPage = productCard.detailPage,
            productCode = productCard.productCode,
            price = productCard.price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = DanawaDetailParser.normalizeOs(spec["운영체제(OS)"] ?: summaryFallback.os),
            screenSize = DanawaDetailParser.parseScreenSize(spec["화면 크기"]) ?: summaryFallback.screenSize,
            resolution = spec["해상도"] ?: summaryFallback.resolution,
            brightness = DanawaDetailParser.parseIntValue(spec["밝기"]) ?: summaryFallback.brightness,
            refreshRate = DanawaDetailParser.parseIntValue(spec["주사율"]) ?: summaryFallback.refreshRate ?: DEFAULT_REFRESH_RATE,
            ramSize = DanawaDetailParser.parseCapacityInGb(spec["램"]) ?: summaryFallback.ramSize,
            ramType = spec["램 타입"] ?: summaryFallback.ramType,
            isRamReplaceable = DanawaDetailParser.parsePossible(spec["램 교체"]) ?: summaryFallback.isRamReplaceable,
            graphicsType = gpuModel,
            tgp = DanawaDetailParser.parseIntValue(spec["TGP"])
                ?: summaryFallback.tgp
                ?: if (isIntegratedGraphics(gpuKind, gpuModel)) 0 else null,
            thunderboltCount = DanawaDetailParser.parseThunderboltCount(spec),
            usbCCount = DanawaDetailParser.parseUsbCCount(spec),
            usbACount = DanawaDetailParser.parseCountValue(spec["USB-A"]),
            sdCard = DanawaDetailParser.parseSdCard(spec),
            isSupportsPdCharging = when {
                spec["전원"] != null -> spec["전원"]!!.contains("USB-PD", ignoreCase = true)
                else -> summaryFallback.isSupportsPdCharging
            },
            batteryCapacity = DanawaDetailParser.parseDoubleValue(spec["배터리"]) ?: summaryFallback.batteryCapacity,
            storageCapacity = DanawaDetailParser.parseCapacityInGb(spec["용량"]) ?: summaryFallback.storageCapacity,
            storageSlotCount = DanawaDetailParser.parseCountValue(spec["저장 슬롯"]) ?: summaryFallback.storageSlotCount,
            weight = DanawaDetailParser.parseWeightValue(spec["무게"]) ?: summaryFallback.weight,
            lastDetailedCrawledAt = LocalDateTime.now(),
            laptopUsage = mutableListOf(),
        )

        laptop.laptopUsage = usages
            .distinct()
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()

        return laptop
    }

    @Transactional
    internal fun saveOrUpdateLaptop(laptop: Laptop): SaveResult {
        return saveOrUpdateLaptop(laptop, null)
    }

    @Transactional
    private fun saveOrUpdateLaptop(laptop: Laptop, existingLaptopHint: Laptop?): SaveResult {
        val existingLaptop = existingLaptopHint ?: findExistingLaptop(laptop)

        if (existingLaptop == null) {
            val savedLaptop = laptopRepository.save(laptop)
            laptopProfileService.syncProfile(savedLaptop)
            laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice = null)
            return SaveResult.CREATED
        }

        val previousPrice = existingLaptop.price
        var changed = false

        changed = updateTextField(existingLaptop.name, laptop.name) { existingLaptop.name = it } || changed
        changed = updateTextField(existingLaptop.imageUrl, laptop.imageUrl) { existingLaptop.imageUrl = it } || changed
        changed = updateTextField(existingLaptop.detailPage, laptop.detailPage) { existingLaptop.detailPage = it } || changed
        changed = updateTextField(existingLaptop.productCode, laptop.productCode) { existingLaptop.productCode = it } || changed
        changed = updatePresentField(existingLaptop.price, laptop.price) { existingLaptop.price = it } || changed
        changed = updateTextField(existingLaptop.cpuManufacturer, laptop.cpuManufacturer) { existingLaptop.cpuManufacturer = it } || changed
        changed = updateTextField(existingLaptop.cpu, laptop.cpu) { existingLaptop.cpu = it } || changed
        changed = updateTextField(existingLaptop.os, laptop.os) { existingLaptop.os = it } || changed
        changed = updatePresentField(existingLaptop.screenSize, laptop.screenSize) { existingLaptop.screenSize = it } || changed
        changed = updateTextField(existingLaptop.resolution, laptop.resolution) { existingLaptop.resolution = it } || changed
        changed = updatePresentField(existingLaptop.brightness, laptop.brightness) { existingLaptop.brightness = it } || changed
        changed = updatePresentField(existingLaptop.refreshRate, laptop.refreshRate) { existingLaptop.refreshRate = it } || changed
        changed = updatePresentField(existingLaptop.ramSize, laptop.ramSize) { existingLaptop.ramSize = it } || changed
        changed = updateTextField(existingLaptop.ramType, laptop.ramType) { existingLaptop.ramType = it } || changed
        changed = updatePresentField(existingLaptop.isRamReplaceable, laptop.isRamReplaceable) { existingLaptop.isRamReplaceable = it } || changed
        changed = updateTextField(existingLaptop.graphicsType, laptop.graphicsType) { existingLaptop.graphicsType = it } || changed
        changed = updatePresentField(existingLaptop.tgp, laptop.tgp) { existingLaptop.tgp = it } || changed
        changed = updatePresentField(existingLaptop.thunderboltCount, laptop.thunderboltCount) { existingLaptop.thunderboltCount = it } || changed
        changed = updatePresentField(existingLaptop.usbCCount, laptop.usbCCount) { existingLaptop.usbCCount = it } || changed
        changed = updatePresentField(existingLaptop.usbACount, laptop.usbACount) { existingLaptop.usbACount = it } || changed
        changed = updateTextField(existingLaptop.sdCard, laptop.sdCard) { existingLaptop.sdCard = it } || changed
        changed = updatePresentField(existingLaptop.isSupportsPdCharging, laptop.isSupportsPdCharging) { existingLaptop.isSupportsPdCharging = it } || changed
        changed = updatePresentField(existingLaptop.batteryCapacity, laptop.batteryCapacity) { existingLaptop.batteryCapacity = it } || changed
        changed = updatePresentField(existingLaptop.storageCapacity, laptop.storageCapacity) { existingLaptop.storageCapacity = it } || changed
        changed = updatePresentField(existingLaptop.storageSlotCount, laptop.storageSlotCount) { existingLaptop.storageSlotCount = it } || changed
        changed = updatePresentField(existingLaptop.weight, laptop.weight) { existingLaptop.weight = it } || changed
        changed = updatePresentField(existingLaptop.lastDetailedCrawledAt, laptop.lastDetailedCrawledAt) { existingLaptop.lastDetailedCrawledAt = it } || changed

        val existingUsages = existingLaptop.laptopUsage.map { it.usage }.sorted()
        val newUsages = laptop.laptopUsage.map { it.usage }.sorted()
        if (newUsages.isNotEmpty() && existingUsages != newUsages) {
            existingLaptop.laptopUsage.clear()
            laptop.laptopUsage.forEach { usage ->
                existingLaptop.laptopUsage.add(LaptopUsage(usage = usage.usage, laptop = existingLaptop))
            }
            changed = true
        }

        return if (changed) {
            val savedLaptop = laptopRepository.save(existingLaptop)
            laptopProfileService.syncProfile(savedLaptop)
            laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice)
            SaveResult.UPDATED
        } else {
            SaveResult.UNCHANGED
        }
    }

    internal fun saveListSnapshot(existingLaptop: Laptop, productCard: ProductCard): SaveResult {
        val previousPrice = existingLaptop.price
        var changed = false

        changed = updateTextField(existingLaptop.name, productCard.productName) { existingLaptop.name = it } || changed
        changed = updateTextField(existingLaptop.imageUrl, productCard.imageUrl) { existingLaptop.imageUrl = it } || changed
        changed = updateTextField(existingLaptop.detailPage, productCard.detailPage) { existingLaptop.detailPage = it } || changed
        changed = updateTextField(existingLaptop.productCode, productCard.productCode) { existingLaptop.productCode = it } || changed
        changed = updatePresentField(existingLaptop.price, productCard.price) { existingLaptop.price = it } || changed

        if (!changed) {
            return SaveResult.UNCHANGED
        }

        val savedLaptop = laptopRepository.save(existingLaptop)
        laptopPriceHistoryService.recordCurrentPrice(savedLaptop, previousPrice)
        return SaveResult.UPDATED
    }

    private fun loadExistingLookup(productCards: List<ProductCard>): ExistingLookup {
        if (productCards.isEmpty()) {
            return ExistingLookup(emptyMap(), emptyMap())
        }

        val byProductCode = laptopRepository.findAllByProductCodeIn(productCards.map { it.productCode }.distinct())
            .mapNotNull { laptop -> laptop.productCode?.let { it to laptop } }
            .toMap()
        val byDetailPage = laptopRepository.findAllByDetailPageIn(productCards.map { it.detailPage }.distinct())
            .associateBy { laptop -> laptop.detailPage }

        return ExistingLookup(
            byProductCode = byProductCode,
            byDetailPage = byDetailPage,
        )
    }

    private fun findExistingLaptop(productCard: ProductCard, existingLookup: ExistingLookup): Laptop? {
        return existingLookup.byProductCode[productCard.productCode]
            ?: existingLookup.byDetailPage[productCard.detailPage]
    }

    private fun findExistingLaptop(laptop: Laptop): Laptop? {
        laptop.productCode?.let { productCode ->
            laptopRepository.findByProductCode(productCode)?.let { return it }
            laptopRepository.findAllByDetailPageContaining("pcode=$productCode")
                .singleOrNull()
                ?.let { return it }
        }
        return laptopRepository.findByDetailPage(laptop.detailPage)
    }

    private fun fetchDetailRefreshOutcomes(
        workItems: List<DetailRefreshWorkItem>,
        executor: ExecutorService,
    ): List<DetailRefreshOutcome> {
        if (workItems.isEmpty()) {
            return emptyList()
        }

        return workItems.map { workItem ->
            executor.submit<DetailRefreshOutcome> {
                runCatching {
                    DetailRefreshOutcome(
                        workItem = workItem,
                        buildResult = buildLaptop(workItem.productCard),
                    )
                }.getOrElse { throwable ->
                    DetailRefreshOutcome(
                        workItem = workItem,
                        error = throwable as? Exception ?: IllegalStateException(throwable.message, throwable),
                    )
                }
            }
        }.map { future ->
            future.get()
        }
    }

    private fun recordSample(
        samples: MutableList<String>,
        productCard: ProductCard,
        reason: String,
    ) {
        if (samples.size >= MAX_FAILURE_SAMPLES) {
            return
        }

        samples += "${productCard.productCode} | ${productCard.productName} | $reason"
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

    internal fun resolveFilterProfile(rawValue: String?): FilterProfile {
        return when (rawValue?.trim()?.lowercase()) {
            null, "", "core" -> FilterProfile.CORE
            "none", "all" -> FilterProfile.NONE
            "extended" -> FilterProfile.EXTENDED
            else -> {
                logger.warn("알 수 없는 crawler filter profile='{}'. 기본값 core를 사용합니다.", rawValue)
                FilterProfile.CORE
            }
        }
    }

    internal fun resolveCrawlSources(filterProfile: FilterProfile): List<CrawlSource> {
        val mainSource = when (filterProfile) {
            FilterProfile.NONE -> {
                CrawlSource(
                    key = "notebook-all",
                    listUrl = NOTEBOOK_LIST_URL,
                )
            }
            FilterProfile.CORE -> {
                CrawlSource(
                    key = "notebook-core-codename",
                    listUrl = NOTEBOOK_LIST_URL,
                    attributeFilters = CrawlerFilterSets.coreCpuCodenames,
                )
            }
            FilterProfile.EXTENDED -> {
                CrawlSource(
                    key = "notebook-extended-codename",
                    listUrl = NOTEBOOK_LIST_URL,
                    attributeFilters = CrawlerFilterSets.extendedCpuCodenames,
                )
            }
        }

        return listOf(
            mainSource,
            CrawlSource(
                key = "apple-macbook",
                listUrl = APPLE_MACBOOK_LIST_URL,
            ),
        )
    }

    private fun fetchDetailPageHtml(detailPage: String): String {
        return sendRequest(
            HttpRequest.newBuilder(URI.create(detailPage))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build(),
        )
    }

    private fun fetchDetailSpecHtml(
        productCard: ProductCard,
        detailRequestContext: DetailRequestContext?,
    ): String? {
        if (detailRequestContext?.makerName.isNullOrBlank() ||
            detailRequestContext?.productName.isNullOrBlank() ||
            detailRequestContext?.prodType.isNullOrBlank()
        ) {
            return null
        }

        val context = detailRequestContext ?: return null

        val formData = linkedMapOf(
            "pcode" to productCard.productCode,
            "cate1" to productCard.cate1,
            "cate2" to productCard.cate2,
            "cate3" to productCard.cate3,
            "cate4" to productCard.cate4,
            "makerName" to context.makerName,
            "productName" to context.productName,
            "prodType" to context.prodType,
        )

        val request = HttpRequest.newBuilder(URI.create(PRODUCT_DESCRIPTION_URL))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", FORM_URLENCODED)
            .header("Origin", DANAWA_ORIGIN)
            .header("Referer", productCard.detailPage)
            .header("X-Requested-With", "XMLHttpRequest")
            .POST(HttpRequest.BodyPublishers.ofString(buildFormData(formData)))
            .build()

        val responseBody = try {
            sendRequest(request)
        } catch (e: Exception) {
            logger.warn("상세 스펙 테이블 요청 실패, 요약 스펙으로 대체합니다: {} / {}", productCard.detailPage, e.message)
            return null
        }

        return responseBody.takeIf { it.contains("spec_tbl") }
    }

    private fun sendRequest(request: HttpRequest): String {
        var lastException: Exception? = null

        repeat(MAX_HTTP_RETRIES) { attempt ->
            try {
                awaitRequestSlot()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                if (response.statusCode() in 200..299) {
                    return response.body()
                }

                if (response.statusCode() in RETRYABLE_STATUS_CODES && attempt < MAX_HTTP_RETRIES - 1) {
                    val cooldown = retryDelayMillis(attempt, response.statusCode())
                    extendGlobalCooldown(cooldown)
                    logger.warn(
                        "재시도 가능한 HTTP 상태를 감지해 잠시 대기합니다. status={}, wait={}ms, uri={}",
                        response.statusCode(),
                        cooldown,
                        request.uri(),
                    )
                    return@repeat
                }

                throw IllegalStateException("HTTP ${response.statusCode()} 요청 실패: ${request.uri()}")
            } catch (e: IOException) {
                lastException = e

                if (attempt == MAX_HTTP_RETRIES - 1) {
                    throw e
                }

                val cooldown = retryDelayMillis(attempt)
                extendGlobalCooldown(cooldown)
                logger.warn("I/O 오류로 요청을 재시도합니다. wait={}ms, uri={}, reason={}", cooldown, request.uri(), e.message)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("HTTP 요청이 중단되었습니다: ${request.uri()}", e)
            }
        }

        throw IllegalStateException("HTTP 요청 실패: ${request.uri()}", lastException)
    }

    private fun awaitRequestSlot() {
        while (true) {
            val waitMillis = synchronized(requestPacingLock) {
                val now = System.currentTimeMillis()
                val allowedAt = maxOf(now, nextAllowedRequestAtMillis, globalCooldownUntilMillis)
                if (allowedAt <= now) {
                    nextAllowedRequestAtMillis = now + MIN_REQUEST_INTERVAL_MILLIS + randomJitterMillis(REQUEST_JITTER_MILLIS)
                    0L
                } else {
                    allowedAt - now
                }
            }

            if (waitMillis <= 0L) {
                return
            }

            Thread.sleep(waitMillis)
        }
    }

    private fun extendGlobalCooldown(delayMillis: Long) {
        if (delayMillis <= 0L) {
            return
        }

        synchronized(requestPacingLock) {
            val candidate = System.currentTimeMillis() + delayMillis
            if (candidate > globalCooldownUntilMillis) {
                globalCooldownUntilMillis = candidate
            }
        }
    }

    private fun retryDelayMillis(attempt: Int, statusCode: Int? = null): Long {
        val baseDelay = when (statusCode) {
            429 -> 4_000L
            403 -> 2_500L
            500, 502, 503, 504 -> 1_500L
            else -> RETRY_DELAY_MILLIS
        }
        val exponential = baseDelay * (1L shl attempt.coerceAtMost(4))
        return minOf(MAX_RETRY_DELAY_MILLIS, exponential) + randomJitterMillis(RETRY_JITTER_MILLIS)
    }

    private fun randomJitterMillis(maxJitterMillis: Long): Long {
        if (maxJitterMillis <= 0L) {
            return 0L
        }

        return ThreadLocalRandom.current().nextLong(maxJitterMillis + 1)
    }

    private fun buildFormData(data: Iterable<Pair<String, String?>>): String {
        return data
            .filter { !it.second.isNullOrBlank() }
            .joinToString("&") { (key, value) ->
                "${key.urlEncode()}=${value.orEmpty().urlEncode()}"
            }
    }

    private fun buildFormData(data: Map<String, String?>): String {
        return buildFormData(data.entries.map { it.key to it.value })
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

    private fun resolveCpuManufacturer(rawManufacturer: String?, productName: String, rawCpu: String?): String? {
        rawManufacturer?.let(DanawaDetailParser::normalizeCpuManufacturer)?.let { return it }

        val normalizedName = productName.uppercase()
        val normalizedCpu = rawCpu.orEmpty().uppercase()

        return when {
            normalizedName.contains("APPLE") || normalizedName.contains("맥북") -> "애플(ARM)"
            normalizedName.contains("SNAPDRAGON") ||
                normalizedName.contains("X ELITE") ||
                normalizedName.contains("X PLUS") ||
                normalizedCpu.startsWith("X1") ||
                normalizedCpu.startsWith("X2") -> "퀄컴"
            else -> null
        }
    }

    internal fun resolveCpuModel(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return laptopProfileFactory.resolveCpuToken(rawCpu, cpuManufacturer, productName)
            ?: rawCpu?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun isIntegratedGraphics(graphicsKind: String?, graphicsModel: String?): Boolean {
        val normalizedKind = graphicsKind.orEmpty().uppercase()
        val normalizedModel = graphicsModel.orEmpty().uppercase()

        if (normalizedKind.contains("외장")) {
            return false
        }
        if (normalizedKind.contains("내장")) {
            return true
        }

        if (DISCRETE_GPU_KEYWORDS.any { normalizedModel.contains(it) }) {
            return false
        }
        if (INTEGRATED_GPU_KEYWORDS.any { normalizedModel.contains(it) }) {
            return true
        }

        return false
    }

    private fun updateTextField(currentValue: String?, newValue: String?, updater: (String) -> Unit): Boolean {
        val normalizedValue = newValue?.trim()?.takeIf { it.isNotBlank() } ?: return false
        if (currentValue?.trim() == normalizedValue) {
            return false
        }
        updater(normalizedValue)
        return true
    }

    private fun <T : Any> updatePresentField(currentValue: T?, newValue: T?, updater: (T) -> Unit): Boolean {
        val normalizedValue = newValue ?: return false
        if (currentValue == normalizedValue) {
            return false
        }
        updater(normalizedValue)
        return true
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8)
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
        private const val NOTEBOOK_LIST_URL = "https://prod.danawa.com/list/?cate=112758"
        private const val APPLE_MACBOOK_LIST_URL = "https://prod.danawa.com/list/?cate=11236463"
        private const val LIST_AJAX_URL = "https://prod.danawa.com/list/ajax/getProductList.ajax.php"
        private const val LIST_SORT_METHOD = "MinPrice"
        private const val DANAWA_ORIGIN = "https://prod.danawa.com"
        private const val PRODUCT_DESCRIPTION_URL = "https://prod.danawa.com/info/ajax/getProductDescription.ajax.php"
        private const val FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8"
        private const val DEFAULT_REFRESH_RATE = 60
        private const val MAX_HTTP_RETRIES = 3
        private const val RETRY_DELAY_MILLIS = 800L
        private const val MAX_RETRY_DELAY_MILLIS = 8_000L
        private const val RETRY_JITTER_MILLIS = 400L
        private const val MIN_REQUEST_INTERVAL_MILLIS = 120L
        private const val REQUEST_JITTER_MILLIS = 80L
        private const val MAX_FAILURE_SAMPLES = 10
        private const val MAX_LIST_PAGES = 5000
        private const val MAX_CONSECUTIVE_DUPLICATE_ONLY_PAGES = 5
        private const val DETAIL_FETCH_CONCURRENCY = 6
        private val RETRYABLE_STATUS_CODES = setOf(403, 429, 500, 502, 503, 504)
        private val DISCRETE_GPU_KEYWORDS = listOf(
            "RTX",
            "GTX",
            "GEFORCE",
            "RTX PRO",
            "RTX A",
            "ARC B",
            "ARC A",
            "RADEON RX",
        )
        private val INTEGRATED_GPU_KEYWORDS = listOf(
            "INTEL GRAPHICS",
            "IRIS",
            "UHD",
            "HD GRAPHICS",
            "ARC 130T",
            "ARC 140T",
            "RADEON 890M",
            "RADEON 880M",
            "RADEON 860M",
            "RADEON 840M",
            "RADEON 820M",
            "RADEON 8060S",
            "RADEON 780M",
            "RADEON 760M",
            "RADEON 740M",
            "RADEON 680M",
            "RADEON 660M",
            "RADEON 610M",
            "RADEON GRAPHICS",
            "ADRENO",
        )
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(20)
    }
}
