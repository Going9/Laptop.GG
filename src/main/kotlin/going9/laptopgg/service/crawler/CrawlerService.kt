package going9.laptopgg.service.crawler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopProfileFactory
import going9.laptopgg.service.LaptopProfileService
import org.jsoup.Jsoup
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import org.slf4j.LoggerFactory

@Service
class CrawlerService(
    private val laptopRepository: LaptopRepository,
    private val laptopProfileService: LaptopProfileService,
    private val laptopProfileFactory: LaptopProfileFactory,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val objectMapper = jacksonObjectMapper()

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
    ) {
        fun toFormData(page: Int): Map<String, String> {
            return linkedMapOf(
                "page" to page.toString(),
                "listCategoryCode" to listCategoryCode,
                "categoryCode" to categoryCode,
                "physicsCate1" to physicsCate1,
                "physicsCate2" to physicsCate2,
                "physicsCate3" to physicsCate3,
                "physicsCate4" to physicsCate4,
                "viewMethod" to viewMethod,
                "sortMethod" to sortMethod,
                "listCount" to listCount,
                "group" to group,
                "depth" to depth,
                "brandName" to "",
                "makerName" to "",
                "searchOptionName" to "",
                "sDiscountProductRate" to discountProductRate,
                "sInitialPriceDisplay" to initialPriceDisplay,
                "sPowerLinkKeyword" to "",
                "oCurrentCategoryCode" to "",
                "sMallMinPriceDisplayYN" to mallMinPriceDisplayYn,
                "quickDeliveryCategoryYN" to quickDeliveryCategoryYn,
                "quickDeliveryDisplay" to quickDeliveryDisplay,
                "priceUnitSort" to priceUnitSort,
                "priceUnitSortOrder" to priceUnitSortOrder,
                "simpleDescriptionDisplayYN" to simpleDescriptionDisplayYn,
                "simpleDescriptionOpen" to simpleDescriptionOpen,
                "listPackageType" to listPackageType,
                "categoryMappingCode" to "",
                "priceUnit" to priceUnit,
                "priceUnitValue" to priceUnitValue,
                "priceUnitClass" to priceUnitClass,
                "cmRecommendSort" to cmRecommendSort,
                "cmRecommendSortDefault" to cmRecommendSortDefault,
                "bundleImagePreview" to bundleImagePreview,
                "nPackageLimit" to packageLimit,
                "bMakerDisplayYN" to makerDisplayYn,
                "dnwSwitchOn" to "",
                "isDpgZoneUICategory" to dpgZoneUiCategory,
                "isAssemblyGalleryCategory" to assemblyGalleryCategory,
            )
        }
    }

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

    private enum class SaveResult {
        CREATED,
        UPDATED,
        UNCHANGED,
    }

    fun crawlAll(limit: Int? = null): CrawlSummary {
        val initialListHtml = fetchListPageHtml()
        val listRequestContext = extractListRequestContext(initialListHtml)
        laptopProfileService.syncMissingProfilesIfNeeded()
        val detailFetchExecutor = Executors.newFixedThreadPool(DETAIL_FETCH_CONCURRENCY)
        val seenProductCodes = linkedSetOf<String>()
        var currentPage = 1
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

        try {
            while (currentPage <= MAX_LIST_PAGES) {
                val pageStartTime = System.currentTimeMillis()
                val productCards = fetchProductCards(currentPage, listRequestContext, initialListHtml)

                if (productCards.isEmpty()) {
                    logger.info("현재 페이지에서 수집 가능한 상품이 없어 크롤링을 종료합니다. page={}", currentPage)
                    break
                }

                val freshProductCards = productCards.filter { seenProductCodes.add(it.productCode) }
                if (freshProductCards.isEmpty()) {
                    logger.info("이미 수집한 상품만 반복되어 크롤링을 종료합니다. page={}", currentPage)
                    break
                }

                val remainingQuota = limit?.let { (it - processedCount).coerceAtLeast(0) }
                if (remainingQuota == 0) {
                    break
                }

                val candidateProductCards = remainingQuota?.let(freshProductCards::take) ?: freshProductCards
                processedCount += candidateProductCards.size

                val existingLookup = loadExistingLookup(candidateProductCards)
                val detailRefreshWorkItems = mutableListOf<DetailRefreshWorkItem>()

                for (productCard in candidateProductCards) {
                    val existingLaptop = findExistingLaptop(productCard, existingLookup)
                    if (existingLaptop != null && !needsDetailRefresh(existingLaptop)) {
                        try {
                            when (saveListSnapshot(existingLaptop, productCard)) {
                                SaveResult.UPDATED -> {
                                    updatedCount++
                                    priceOnlyUpdatedCount++
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

                logger.info(
                    "페이지 처리 시간: ${System.currentTimeMillis() - pageStartTime}ms / page=${currentPage} / " +
                        "수집 상품: ${productCards.size}개 / 신규 상품: ${freshProductCards.size}개 / 실제 처리: ${candidateProductCards.size}개 / " +
                        "상세 재수집: ${detailRefreshWorkItems.size}개 / 가격만 갱신: ${priceOnlyUpdatedCount}개 / " +
                        "누적 처리: ${processedCount}개 / 누적 열화: ${degradedCount}개 / 누적 실패: ${failedCount}개",
                )

                if (reachedLimit) {
                    break
                }

                currentPage++
            }

            if (currentPage > MAX_LIST_PAGES) {
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

    private fun fetchListPageHtml(): String {
        val request = HttpRequest.newBuilder(URI.create(LIST_URL))
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
            .header("Referer", LIST_URL)
            .header("X-Requested-With", "XMLHttpRequest")
            .POST(HttpRequest.BodyPublishers.ofString(buildFormData(listRequestContext.toFormData(page))))
            .build()

        return sendRequest(request)
    }

    private fun fetchProductCards(
        page: Int,
        listRequestContext: ListRequestContext,
        initialListHtml: String,
    ): List<ProductCard> {
        val html = if (page == 1) {
            initialListHtml
        } else {
            fetchListPageHtml(page, listRequestContext)
        }

        return parseListPage(html)
    }

    internal fun extractListRequestContext(initialListHtml: String): ListRequestContext {
        val defaults = ListRequestContext()

        return ListRequestContext(
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
            simpleDescriptionDisplayYn = extractJsScalar(initialListHtml, "simpleDescriptionDisplayYN") ?: defaults.simpleDescriptionDisplayYn,
            simpleDescriptionOpen = extractJsScalar(initialListHtml, "simpleDescriptionOpen") ?: defaults.simpleDescriptionOpen,
            listPackageType = extractJsScalar(initialListHtml, "nPriceCompareListPackageType") ?: defaults.listPackageType,
            priceUnit = extractJsScalar(initialListHtml, "nPriceUnit") ?: defaults.priceUnit,
            priceUnitValue = extractJsScalar(initialListHtml, "nPriceUnitValue") ?: defaults.priceUnitValue,
            priceUnitClass = extractJsScalar(initialListHtml, "sPriceUnitClass") ?: defaults.priceUnitClass,
            cmRecommendSort = extractJsScalar(initialListHtml, "sCmRecommendSort") ?: defaults.cmRecommendSort,
            cmRecommendSortDefault = extractJsScalar(initialListHtml, "sCmRecommendSortDefault") ?: defaults.cmRecommendSortDefault,
            bundleImagePreview = extractJsScalar(initialListHtml, "sBundleImagePreview") ?: defaults.bundleImagePreview,
            packageLimit = extractJsScalar(initialListHtml, "nPriceCompareListPackageLimit") ?: defaults.packageLimit,
            makerDisplayYn = extractJsScalar(initialListHtml, "sMakerStandardDisplayStatus")
                ?: extractJsScalar(initialListHtml, "sMakerIndicate")
                ?: defaults.makerDisplayYn,
            dpgZoneUiCategory = extractJsScalar(initialListHtml, "isDpgZoneUICategory") ?: defaults.dpgZoneUiCategory,
            assemblyGalleryCategory = extractJsScalar(initialListHtml, "isAssemblyGalleryCategory") ?: defaults.assemblyGalleryCategory,
        )
    }

    private fun buildLaptop(productCard: ProductCard): BuildLaptopResult {
        val degradationReasons = mutableListOf<String>()
        val detailPageHtml = fetchDetailPageHtml(productCard.detailPage)
        val detailContext = extractDetailRequestContext(detailPageHtml)
        if (detailContext == null) {
            degradationReasons += "상세 스펙 요청 컨텍스트 없음"
        }
        val detailSpecHtml = fetchDetailSpecHtml(productCard, detailContext)
        if (detailSpecHtml == null) {
            degradationReasons += "상세 스펙 테이블 미수집"
        }
        val parsedSpecTable = detailSpecHtml?.let(::parseSpecTable) ?: ParsedSpecTable(emptyMap(), emptyList())
        if (detailSpecHtml != null && parsedSpecTable.values.isEmpty()) {
            degradationReasons += "상세 스펙 테이블 파싱 결과 비어 있음"
        }
        val summaryFallback = parseSummaryFallback(extractSummaryText(detailPageHtml))
        if (parsedSpecTable.values.isEmpty() && summaryFallback.isEmpty()) {
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
            os = normalizeOs(spec["운영체제(OS)"] ?: summaryFallback.os),
            screenSize = parseScreenSize(spec["화면 크기"]) ?: summaryFallback.screenSize,
            resolution = spec["해상도"] ?: summaryFallback.resolution,
            brightness = parseIntValue(spec["밝기"]) ?: summaryFallback.brightness,
            refreshRate = parseIntValue(spec["주사율"]) ?: summaryFallback.refreshRate ?: DEFAULT_REFRESH_RATE,
            ramSize = parseCapacityInGb(spec["램"]) ?: summaryFallback.ramSize,
            ramType = spec["램 타입"] ?: summaryFallback.ramType,
            isRamReplaceable = parsePossible(spec["램 교체"]) ?: summaryFallback.isRamReplaceable,
            graphicsType = gpuModel,
            tgp = parseIntValue(spec["TGP"])
                ?: summaryFallback.tgp
                ?: if (isIntegratedGraphics(gpuKind, gpuModel)) 0 else null,
            thunderboltCount = parseThunderboltCount(spec),
            usbCCount = parseUsbCCount(spec),
            usbACount = parseCountValue(spec["USB-A"]),
            sdCard = parseSdCard(spec),
            isSupportsPdCharging = when {
                spec["전원"] != null -> spec["전원"]!!.contains("USB-PD", ignoreCase = true)
                else -> summaryFallback.isSupportsPdCharging
            },
            batteryCapacity = parseDoubleValue(spec["배터리"]) ?: summaryFallback.batteryCapacity,
            storageCapacity = parseCapacityInGb(spec["용량"]) ?: summaryFallback.storageCapacity,
            storageSlotCount = parseCountValue(spec["저장 슬롯"]) ?: summaryFallback.storageSlotCount,
            weight = parseWeightValue(spec["무게"]) ?: summaryFallback.weight,
            laptopUsage = mutableListOf(),
        )

        laptop.laptopUsage = usages
            .distinct()
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()

        return laptop
    }

    @Transactional
    private fun saveOrUpdateLaptop(laptop: Laptop): SaveResult {
        return saveOrUpdateLaptop(laptop, null)
    }

    @Transactional
    private fun saveOrUpdateLaptop(laptop: Laptop, existingLaptopHint: Laptop?): SaveResult {
        val existingLaptop = existingLaptopHint ?: findExistingLaptop(laptop)

        if (existingLaptop == null) {
            val savedLaptop = laptopRepository.save(laptop)
            laptopProfileService.syncProfile(savedLaptop)
            return SaveResult.CREATED
        }

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
            SaveResult.UPDATED
        } else {
            SaveResult.UNCHANGED
        }
    }

    private fun saveListSnapshot(existingLaptop: Laptop, productCard: ProductCard): SaveResult {
        var changed = false

        changed = updateTextField(existingLaptop.name, productCard.productName) { existingLaptop.name = it } || changed
        changed = updateTextField(existingLaptop.imageUrl, productCard.imageUrl) { existingLaptop.imageUrl = it } || changed
        changed = updateTextField(existingLaptop.detailPage, productCard.detailPage) { existingLaptop.detailPage = it } || changed
        changed = updateTextField(existingLaptop.productCode, productCard.productCode) { existingLaptop.productCode = it } || changed
        changed = updatePresentField(existingLaptop.price, productCard.price) { existingLaptop.price = it } || changed

        if (!changed) {
            return SaveResult.UNCHANGED
        }

        laptopRepository.save(existingLaptop)
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

    private fun needsDetailRefresh(existingLaptop: Laptop): Boolean {
        return existingLaptop.cpuManufacturer.isNullOrBlank() ||
            existingLaptop.cpu.isNullOrBlank() ||
            existingLaptop.os.isNullOrBlank() ||
            existingLaptop.screenSize == null ||
            existingLaptop.resolution.isNullOrBlank() ||
            existingLaptop.ramSize == null ||
            existingLaptop.graphicsType.isNullOrBlank() ||
            existingLaptop.storageCapacity == null ||
            existingLaptop.batteryCapacity == null ||
            existingLaptop.weight == null ||
            existingLaptop.laptopUsage.isEmpty()
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

    internal fun parseListPage(html: String): List<ProductCard> {
        val document = Jsoup.parse(html, LIST_URL)

        return document.select("li.prod_item")
            .mapNotNull { productItem ->
                val productLink = productItem.selectFirst("a[name=productName]") ?: return@mapNotNull null
                val detailPage = productLink.attr("href").trim()
                val productCode = extractQueryParam(detailPage, "pcode") ?: return@mapNotNull null
                val cateValues = productItem.selectFirst(".prod_pricelist")
                    ?.attr("data-cate")
                    ?.split("|")
                    ?.map { it.trim() }
                    ?.takeIf { it.size == 4 }
                    ?: return@mapNotNull null

                val imageElement = productItem.selectFirst(".thumb_image img")
                val imageUrl = normalizeImageUrl(
                    imageElement?.attr("src")
                        ?.takeIf { it.isNotBlank() && !it.contains("noImg") }
                        ?: imageElement?.attr("data-original").orEmpty(),
                )

                val priceText = productItem.selectFirst(".prod_pricelist .text__number")?.text()
                    ?: productItem.selectFirst(".price_sect a")?.text()

                ProductCard(
                    productCode = productCode,
                    productName = productLink.text().trim(),
                    detailPage = normalizeDetailPage(detailPage, productCode, cateValues[3]),
                    imageUrl = imageUrl,
                    price = parsePrice(priceText),
                    cate1 = cateValues[0],
                    cate2 = cateValues[1],
                    cate3 = cateValues[2],
                    cate4 = cateValues[3],
                )
            }
            .distinctBy { it.productCode }
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

    internal fun extractDetailRequestContext(detailPageHtml: String): DetailRequestContext? {
        val match = PRODUCT_DESCRIPTION_INFO_REGEX.find(detailPageHtml) ?: return null
        val infoMap = objectMapper.readValue(match.groupValues[1], Map::class.java)
            .mapNotNull { (key, value) ->
                val stringKey = key as? String ?: return@mapNotNull null
                val stringValue = value as? String ?: return@mapNotNull null
                stringKey to stringValue
            }
            .toMap()

        return DetailRequestContext(
            makerName = infoMap["makerName"]?.trim(),
            productName = infoMap["productName"]?.trim(),
            prodType = infoMap["prodType"]?.trim(),
        )
    }

    private fun extractSummaryText(detailPageHtml: String): String {
        return Jsoup.parse(detailPageHtml, DANAWA_ORIGIN)
            .selectFirst(".summary_info .spec_list")
            ?.text()
            .orEmpty()
    }

    internal fun parseSpecTable(html: String): ParsedSpecTable {
        val document = Jsoup.parse(html, DANAWA_ORIGIN)
        val specTable = document.selectFirst("table.spec_tbl") ?: return ParsedSpecTable(emptyMap(), emptyList())

        val values = linkedMapOf<String, String>()
        val usages = mutableListOf<String>()
        var currentSection: String? = null

        specTable.select("tr").forEach { row ->
            val children = row.children()
            if (children.isEmpty()) {
                return@forEach
            }

            if (children.size == 1 && children.first()?.tagName() == "th" && row.select("td").isEmpty()) {
                currentSection = children.first()!!.text().trim()
                return@forEach
            }

            var index = 0
            while (index + 1 < children.size) {
                val keyCell = children[index]
                val valueCell = children.getOrNull(index + 1)

                if (keyCell.tagName() == "th" && valueCell?.tagName() == "td") {
                    val key = keyCell.text().trim()
                    val value = valueCell.text().trim()

                    if (key.isNotBlank()) {
                        if (currentSection == "용도" && value == "○") {
                            usages += key
                        } else if (value.isNotBlank()) {
                            values[key] = value
                        }
                    }
                }

                index += 2
            }
        }

        return ParsedSpecTable(
            values = values,
            usages = usages.distinct(),
        )
    }

    internal fun parseSummaryFallback(summaryText: String): SummaryFallback {
        val normalizedText = summaryText.replace(Regex("\\s+"), " ").trim()
        if (normalizedText.isBlank()) {
            return SummaryFallback()
        }

        return SummaryFallback(
            cpuManufacturer = extractFirst(normalizedText, Regex("""\[CPU\]\s*(인텔|Intel|AMD|APPLE|Apple|퀄컴|Qualcomm)""", RegexOption.IGNORE_CASE))
                ?.let(::normalizeCpuManufacturer),
            cpu = extractFirst(normalizedText, Regex("""\[CPU\][^\[]*?/\s*([A-Za-z0-9\-]+)\s*\(""", RegexOption.IGNORE_CASE)),
            os = extractFirst(
                normalizedText,
                Regex("""(OS미포함\(프리도스\)|윈도우11홈|윈도우11프로|윈도우11|윈도우10 프로|윈도우10|macOS|리눅스|크롬OS|Whale OS)"""),
            ),
            screenSize = parseScreenSize(extractFirst(normalizedText, Regex("""([0-9.]+cm\([0-9.]+인치\))"""))),
            resolution = extractFirst(normalizedText, Regex("""해상도\s*:\s*([0-9]+x[0-9]+\([^)]+\))""")),
            brightness = parseIntValue(extractFirst(normalizedText, Regex("""밝기\s*:\s*([0-9]+nit)"""))),
            refreshRate = parseIntValue(extractFirst(normalizedText, Regex("""주사율\s*:\s*([0-9]+Hz)"""))),
            ramSize = parseCapacityInGb(extractFirst(normalizedText, Regex("""램\s*:\s*([0-9]+GB)"""))),
            isRamReplaceable = parsePossible(extractFirst(normalizedText, Regex("""램 교체\s*:\s*(가능|불가능)"""))),
            graphicsKind = extractFirst(normalizedText, Regex("""\[그래픽\]\s*([^/]+)"""))?.trim(),
            graphicsModel = extractFirst(normalizedText, Regex("""\[그래픽\]\s*[^/]+/\s*([^/]+)"""))?.trim(),
            tgp = parseIntValue(extractFirst(normalizedText, Regex("""TGP\s*:\s*([0-9]+W)"""))),
            isSupportsPdCharging = normalizedText.contains("USB-PD"),
            batteryCapacity = parseDoubleValue(extractFirst(normalizedText, Regex("""배터리\s*:\s*([0-9.]+Wh)"""))),
            storageCapacity = parseCapacityInGb(extractFirst(normalizedText, Regex("""용량\s*:\s*([0-9.]+(?:TB|GB))"""))),
            storageSlotCount = parseCountValue(extractFirst(normalizedText, Regex("""저장 슬롯\s*:\s*([0-9]+개)"""))),
            weight = parseWeightValue(normalizedText),
            usages = extractFirst(normalizedText, Regex("""용도\s*:\s*([^\[]+)"""))
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
        )
    }

    private fun sendRequest(request: HttpRequest): String {
        var lastException: Exception? = null

        repeat(MAX_HTTP_RETRIES) { attempt ->
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                if (response.statusCode() !in 200..299) {
                    throw IllegalStateException("HTTP ${response.statusCode()} 요청 실패: ${request.uri()}")
                }
                return response.body()
            } catch (e: IOException) {
                lastException = e

                if (attempt == MAX_HTTP_RETRIES - 1) {
                    throw e
                }

                Thread.sleep(RETRY_DELAY_MILLIS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("HTTP 요청이 중단되었습니다: ${request.uri()}", e)
            }
        }

        throw IllegalStateException("HTTP 요청 실패: ${request.uri()}", lastException)
    }

    private fun buildFormData(data: Map<String, String?>): String {
        return data.entries
            .filter { !it.value.isNullOrBlank() }
            .joinToString("&") { (key, value) ->
                "${key.urlEncode()}=${value.orEmpty().urlEncode()}"
            }
    }

    private fun normalizeImageUrl(url: String): String {
        if (url.isBlank()) {
            return ""
        }

        val normalizedUrl = when {
            url.startsWith("//") -> "https:$url"
            else -> url
        }

        return normalizedUrl.replace(Regex("shrink=\\d+:\\d+"), "shrink=500:500")
    }

    private fun parsePrice(priceText: String?): Int? {
        return priceText
            ?.replace(Regex("[^0-9]"), "")
            ?.takeIf { it.isNotBlank() }
            ?.toIntOrNull()
    }

    private fun normalizeDetailPage(rawDetailPage: String, productCode: String, categoryCode: String): String {
        val cate = extractQueryParam(rawDetailPage, "cate") ?: categoryCode
        return "$DANAWA_ORIGIN/info/?pcode=$productCode&cate=$cate"
    }

    private fun parseScreenSize(value: String?): Int? {
        val inches = Regex("""([0-9.]+)인치""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            ?: return null
        return inches.toInt()
    }

    private fun parseIntValue(value: String?): Int? {
        return Regex("""([0-9]+)""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun parseDoubleValue(value: String?): Double? {
        return Regex("""([0-9]+(?:\.[0-9]+)?)""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    internal fun parseWeightValue(value: String?): Double? {
        val text = value.orEmpty()
        val kilogramWeights = Regex("""([0-9]+(?:\.[0-9]+)?)\s*kg""", RegexOption.IGNORE_CASE)
            .findAll(text)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .toList()
        val gramWeights = Regex("""([0-9]+(?:\.[0-9]+)?)\s*g""", RegexOption.IGNORE_CASE)
            .findAll(text)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.toDoubleOrNull()?.div(1000.0) }
            .toList()

        return (kilogramWeights + gramWeights)
            .filter { it > 0 }
            .maxOrNull()
    }

    private fun parseCapacityInGb(value: String?): Int? {
        val match = Regex("""([0-9]+(?:\.[0-9]+)?)(TB|GB)""", RegexOption.IGNORE_CASE).find(value.orEmpty()) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].uppercase()

        return when (unit) {
            "TB" -> (amount * 1024).roundToInt()
            "GB" -> amount.roundToInt()
            else -> null
        }
    }

    private fun parseCountValue(value: String?): Int? {
        return Regex("""([0-9]+)개""").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun parsePossible(value: String?): Boolean? {
        return when (value?.trim()) {
            "가능" -> true
            "불가능" -> false
            else -> null
        }
    }

    private fun parseThunderboltCount(spec: Map<String, String>): Int? {
        val count = spec.entries
            .filter { it.key.startsWith("썬더볼트") }
            .sumOf { parseCountValue(it.value) ?: 0 }

        return count.takeIf { it > 0 }
    }

    private fun parseUsbCCount(spec: Map<String, String>): Int? {
        val directCount = parseCountValue(spec["USB-C"])
        if (directCount != null) {
            return directCount
        }

        val thunderboltCount = spec.entries
            .filter { it.key.startsWith("썬더볼트") && it.value.contains("USB-C겸용") }
            .sumOf { parseCountValue(it.value) ?: 0 }

        return thunderboltCount.takeIf { it > 0 }
    }

    private fun parseSdCard(spec: Map<String, String>): String? {
        return when {
            spec["SD카드"] == "○" -> "SD카드"
            spec["MicroSD카드"] == "○" -> "MicroSD카드"
            else -> null
        }
    }

    private fun normalizeOs(rawOs: String?): String? {
        val value = rawOs?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (value.contains("미포함")) "freedos" else value
    }

    private fun resolveCpuManufacturer(rawManufacturer: String?, productName: String, rawCpu: String?): String? {
        rawManufacturer?.let(::normalizeCpuManufacturer)?.let { return it }

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

    private fun normalizeCpuManufacturer(rawManufacturer: String): String {
        return when {
            rawManufacturer.contains("intel", ignoreCase = true) || rawManufacturer.contains("인텔") -> "인텔"
            rawManufacturer.contains("amd", ignoreCase = true) -> "AMD"
            rawManufacturer.contains("apple", ignoreCase = true) || rawManufacturer.contains("애플") -> "애플(ARM)"
            rawManufacturer.contains("qualcomm", ignoreCase = true) || rawManufacturer.contains("퀄컴") -> "퀄컴"
            else -> rawManufacturer.trim()
        }
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

    private fun SummaryFallback.isEmpty(): Boolean {
        return cpuManufacturer == null &&
            cpu == null &&
            os == null &&
            screenSize == null &&
            resolution == null &&
            brightness == null &&
            refreshRate == null &&
            ramSize == null &&
            ramType == null &&
            isRamReplaceable == null &&
            graphicsKind == null &&
            graphicsModel == null &&
            tgp == null &&
            isSupportsPdCharging == null &&
            batteryCapacity == null &&
            storageCapacity == null &&
            storageSlotCount == null &&
            weight == null &&
            usages.isEmpty()
    }

    private fun extractQueryParam(url: String, key: String): String? {
        return Regex("""(?:\?|&)$key=([^&]+)""").find(url)?.groupValues?.getOrNull(1)
    }

    private fun extractFirst(text: String, regex: Regex): String? {
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
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

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8)
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
        private const val LIST_URL = "https://prod.danawa.com/list/?cate=112758"
        private const val LIST_AJAX_URL = "https://prod.danawa.com/list/ajax/getProductList.ajax.php"
        private const val DANAWA_ORIGIN = "https://prod.danawa.com"
        private const val PRODUCT_DESCRIPTION_URL = "https://prod.danawa.com/info/ajax/getProductDescription.ajax.php"
        private const val FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8"
        private const val DEFAULT_REFRESH_RATE = 60
        private const val MAX_HTTP_RETRIES = 3
        private const val RETRY_DELAY_MILLIS = 500L
        private const val MAX_FAILURE_SAMPLES = 10
        private const val MAX_LIST_PAGES = 5000
        private const val DETAIL_FETCH_CONCURRENCY = 6
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
        private val PRODUCT_DESCRIPTION_INFO_REGEX = Regex(
            """var\s+oProductDescriptionInfo\s*=\s*(\{.*?});""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
    }
}
