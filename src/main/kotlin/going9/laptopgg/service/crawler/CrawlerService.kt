package going9.laptopgg.service.crawler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.service.LaptopProfileFactory
import going9.laptopgg.service.LaptopProfileService
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.ObjectProvider
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
import kotlin.math.roundToInt

@Service
class CrawlerService(
    private val webDriverProvider: ObjectProvider<WebDriver>,
    private val laptopRepository: LaptopRepository,
    private val laptopProfileService: LaptopProfileService,
    private val laptopProfileFactory: LaptopProfileFactory,
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val objectMapper = jacksonObjectMapper()

    data class CrawlSummary(
        val processedCount: Int,
        val createdCount: Int,
        val updatedCount: Int,
    )

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

    private enum class SaveResult {
        CREATED,
        UPDATED,
        UNCHANGED,
    }

    fun crawlAll(limit: Int? = null): CrawlSummary {
        val webDriver = webDriverProvider.getObject()
        val browserCrawler = BrowserCrawler(webDriver)

        return try {
            browserCrawler.loadLaptopPage()
            browserCrawler.setupFilters()

            var processedCount = 0
            var createdCount = 0
            var updatedCount = 0
            var reachedLimit = false

            while (true) {
                val pageStartTime = System.currentTimeMillis()
                val productCards = browserCrawler.currentProductCards()

                if (productCards.isEmpty()) {
                    println("현재 페이지에서 수집 가능한 상품이 없어 크롤링을 종료합니다.")
                    break
                }

                for (productCard in productCards) {
                    val laptop = buildLaptop(productCard) ?: continue

                    when (saveOrUpdateLaptop(laptop)) {
                        SaveResult.CREATED -> createdCount++
                        SaveResult.UPDATED -> updatedCount++
                        SaveResult.UNCHANGED -> Unit
                    }

                    processedCount++

                    if (limit != null && processedCount >= limit) {
                        reachedLimit = true
                        break
                    }
                }

                println(
                    "페이지 처리 시간: ${System.currentTimeMillis() - pageStartTime}ms / " +
                        "수집 상품: ${productCards.size}개 / 누적 처리: ${processedCount}개",
                )

                if (reachedLimit || !browserCrawler.navigateToNextPage()) {
                    break
                }
            }

            CrawlSummary(
                processedCount = processedCount,
                createdCount = createdCount,
                updatedCount = updatedCount,
            )
        } finally {
            runCatching { webDriver.quit() }
        }
    }

    private fun buildLaptop(productCard: ProductCard): Laptop? {
        return try {
            val detailPageHtml = fetchDetailPageHtml(productCard.detailPage)
            val detailContext = extractDetailRequestContext(detailPageHtml)
            val detailSpecHtml = fetchDetailSpecHtml(productCard, detailContext)
            val parsedSpecTable = detailSpecHtml?.let(::parseSpecTable) ?: ParsedSpecTable(emptyMap(), emptyList())
            val summaryFallback = parseSummaryFallback(extractSummaryText(detailPageHtml))

            createLaptop(productCard, parsedSpecTable, summaryFallback)
        } catch (e: Exception) {
            println("제품 상세 수집 중 오류 발생: ${productCard.detailPage} / ${e.message}")
            e.printStackTrace()
            null
        }
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
        return try {
            val existingLaptop = laptopRepository.findByDetailPage(laptop.detailPage)
                ?: laptopRepository.findByName(laptop.name)

            if (existingLaptop == null) {
                val savedLaptop = laptopRepository.save(laptop)
                laptopProfileService.syncProfile(savedLaptop)
                SaveResult.CREATED
            } else {
                var changed = false

                changed = updateField(existingLaptop.name, laptop.name) { existingLaptop.name = it } || changed
                changed = updateField(existingLaptop.imageUrl, laptop.imageUrl) { existingLaptop.imageUrl = it } || changed
                changed = updateField(existingLaptop.detailPage, laptop.detailPage) { existingLaptop.detailPage = it } || changed
                changed = updateField(existingLaptop.price, laptop.price) { existingLaptop.price = it } || changed
                changed = updateField(existingLaptop.cpuManufacturer, laptop.cpuManufacturer) { existingLaptop.cpuManufacturer = it } || changed
                changed = updateField(existingLaptop.cpu, laptop.cpu) { existingLaptop.cpu = it } || changed
                changed = updateField(existingLaptop.os, laptop.os) { existingLaptop.os = it } || changed
                changed = updateField(existingLaptop.screenSize, laptop.screenSize) { existingLaptop.screenSize = it } || changed
                changed = updateField(existingLaptop.resolution, laptop.resolution) { existingLaptop.resolution = it } || changed
                changed = updateField(existingLaptop.brightness, laptop.brightness) { existingLaptop.brightness = it } || changed
                changed = updateField(existingLaptop.refreshRate, laptop.refreshRate) { existingLaptop.refreshRate = it } || changed
                changed = updateField(existingLaptop.ramSize, laptop.ramSize) { existingLaptop.ramSize = it } || changed
                changed = updateField(existingLaptop.ramType, laptop.ramType) { existingLaptop.ramType = it } || changed
                changed = updateField(existingLaptop.isRamReplaceable, laptop.isRamReplaceable) { existingLaptop.isRamReplaceable = it } || changed
                changed = updateField(existingLaptop.graphicsType, laptop.graphicsType) { existingLaptop.graphicsType = it } || changed
                changed = updateField(existingLaptop.tgp, laptop.tgp) { existingLaptop.tgp = it } || changed
                changed = updateField(existingLaptop.thunderboltCount, laptop.thunderboltCount) { existingLaptop.thunderboltCount = it } || changed
                changed = updateField(existingLaptop.usbCCount, laptop.usbCCount) { existingLaptop.usbCCount = it } || changed
                changed = updateField(existingLaptop.usbACount, laptop.usbACount) { existingLaptop.usbACount = it } || changed
                changed = updateField(existingLaptop.sdCard, laptop.sdCard) { existingLaptop.sdCard = it } || changed
                changed = updateField(existingLaptop.isSupportsPdCharging, laptop.isSupportsPdCharging) { existingLaptop.isSupportsPdCharging = it } || changed
                changed = updateField(existingLaptop.batteryCapacity, laptop.batteryCapacity) { existingLaptop.batteryCapacity = it } || changed
                changed = updateField(existingLaptop.storageCapacity, laptop.storageCapacity) { existingLaptop.storageCapacity = it } || changed
                changed = updateField(existingLaptop.storageSlotCount, laptop.storageSlotCount) { existingLaptop.storageSlotCount = it } || changed
                changed = updateField(existingLaptop.weight, laptop.weight) { existingLaptop.weight = it } || changed

                val existingUsages = existingLaptop.laptopUsage.map { it.usage }.sorted()
                val newUsages = laptop.laptopUsage.map { it.usage }.sorted()
                if (existingUsages != newUsages) {
                    existingLaptop.laptopUsage.clear()
                    laptop.laptopUsage.forEach { usage ->
                        existingLaptop.laptopUsage.add(LaptopUsage(usage = usage.usage, laptop = existingLaptop))
                    }
                    changed = true
                }

                if (changed) {
                    val savedLaptop = laptopRepository.save(existingLaptop)
                    laptopProfileService.syncProfile(savedLaptop)
                    SaveResult.UPDATED
                } else {
                    laptopProfileService.syncProfile(existingLaptop)
                    SaveResult.UNCHANGED
                }
            }
        } catch (e: Exception) {
            println("데이터베이스 작업 중 오류 발생: ${e.message}")
            e.printStackTrace()
            SaveResult.UNCHANGED
        }
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
                    detailPage = detailPage,
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
            println("상세 스펙 테이블 요청 실패, 요약 스펙으로 대체합니다: ${productCard.detailPage} / ${e.message}")
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
        if (graphicsKind?.contains("내장", ignoreCase = true) == true) {
            return true
        }

        val normalizedModel = graphicsModel.orEmpty()
        return listOf("Iris", "Arc", "Radeon", "UHD", "Graphics").any { keyword ->
            normalizedModel.contains(keyword, ignoreCase = true)
        }
    }

    private fun <T> updateField(currentValue: T, newValue: T, updater: (T) -> Unit): Boolean {
        if (currentValue == newValue) {
            return false
        }
        updater(newValue)
        return true
    }

    private fun extractQueryParam(url: String, key: String): String? {
        return Regex("""(?:\?|&)$key=([^&]+)""").find(url)?.groupValues?.getOrNull(1)
    }

    private fun extractFirst(text: String, regex: Regex): String? {
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8)
    }

    private inner class BrowserCrawler(
        private val webDriver: WebDriver,
    ) {
        private val wait = WebDriverWait(webDriver, Duration.ofSeconds(20))
        private val shortWait = WebDriverWait(webDriver, Duration.ofSeconds(5))

        fun loadLaptopPage() {
            webDriver.get(LIST_URL)
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.prod_item")))
            println("페이지 타이틀: ${webDriver.title}")
        }

        fun setupFilters() {
            expandCpuCodeSection()
            selectCpuAttributes()
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.prod_item")))
        }

        fun currentProductCards(): List<ProductCard> {
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.prod_item")))
            return parseListPage(webDriver.pageSource)
        }

        fun navigateToNextPage(): Boolean {
            val currentPage = saveCurrentPage()
            val nextPage = currentPage + 1
            val nextPageButton = findPageButton(nextPage) ?: findNextGroupButton()

            if (nextPageButton == null) {
                println("마지막 페이지에 도달하여 크롤링을 종료합니다.")
                return false
            }

            scrollIntoView(nextPageButton)
            nextPageButton.click()

            return try {
                wait.until(ExpectedConditions.textToBe(By.cssSelector(".num.now_on"), nextPage.toString()))
                true
            } catch (_: TimeoutException) {
                println("다음 페이지 이동에 실패하여 크롤링을 종료합니다.")
                false
            }
        }

        private fun expandCpuCodeSection() {
            val expandButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[normalize-space()='CPU 코드명']/ancestor::dl[1]//button[contains(@class, 'btn_view_more')]"),
                ),
            )
            scrollIntoView(expandButton)
            expandButton.click()
        }

        private fun selectCpuAttributes() {
            val cpuCodeNames = listOf(
                "고르곤 포인트",
                "팬서레이크",
                "오라이온 3세대",
                "파이어 레인지",
                "크라켄 포인트",
                "애로우레이크",
                "루나레이크",
                "스트릭스 헤일로",
                "스트릭스 포인트",
                "호크포인트",
                "랩터레이크-R",
                "메테오레이크",
                "트윈레이크",
                "드래곤 레인지",
                "피닉스",
                "랩터레이크",
                "램브란트-R",
                "램브란트",
                "엘더레이크",
                "엘더레이크-N",
                "오라이온",
                "바르셀로-R",
                "바르셀로",
                "루시엔",
                "세잔",
            )

            cpuCodeNames.forEach { cpuCodeName ->
                val label = findOptionalElement(
                    By.xpath("//label[@title=\"$cpuCodeName\"]"),
                ) ?: return@forEach

                val checkbox = label.findElement(By.cssSelector("input[type='checkbox']"))

                if (!checkbox.isSelected) {
                    scrollIntoView(label)
                    label.click()
                }
            }
        }

        private fun saveCurrentPage(): Int {
            return webDriver.findElement(By.cssSelector(".num.now_on")).text.toInt()
        }

        private fun findPageButton(page: Int): WebElement? {
            return findOptionalElement(
                By.xpath("//div[contains(@class, 'prod_num_nav')]//div[contains(@class, 'number_wrap')]//a[normalize-space()='$page']"),
            )
        }

        private fun findNextGroupButton(): WebElement? {
            return findOptionalElement(By.cssSelector(".prod_num_nav .edge_nav.nav_next"))
        }

        private fun findOptionalElement(by: By): WebElement? {
            return try {
                shortWait.until(ExpectedConditions.presenceOfElementLocated(by))
            } catch (_: TimeoutException) {
                null
            } catch (_: NoSuchElementException) {
                null
            } catch (_: StaleElementReferenceException) {
                null
            }
        }

        private fun scrollIntoView(element: WebElement) {
            (webDriver as JavascriptExecutor).executeScript(
                "arguments[0].scrollIntoView({block: 'center'});",
                element,
            )
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
        private const val LIST_URL = "https://prod.danawa.com/list/?cate=112758"
        private const val DANAWA_ORIGIN = "https://prod.danawa.com"
        private const val PRODUCT_DESCRIPTION_URL = "https://prod.danawa.com/info/ajax/getProductDescription.ajax.php"
        private const val FORM_URLENCODED = "application/x-www-form-urlencoded; charset=UTF-8"
        private const val DEFAULT_REFRESH_RATE = 60
        private const val MAX_HTTP_RETRIES = 3
        private const val RETRY_DELAY_MILLIS = 500L
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(20)
        private val PRODUCT_DESCRIPTION_INFO_REGEX = Regex(
            """var\s+oProductDescriptionInfo\s*=\s*(\{.*?});""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
    }
}
