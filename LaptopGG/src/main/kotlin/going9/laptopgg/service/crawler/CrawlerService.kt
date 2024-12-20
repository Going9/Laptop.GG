package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.repository.LaptopRepository
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

// CpuModelMap 파일에서 cpuModelMap을 import
import going9.laptopgg.service.crawler.CpuModelMap.cpuModelMap
import org.springframework.transaction.annotation.Transactional

@Service
class CrawlerService(
    private val webDriver: WebDriver,
    private val laptopRepository: LaptopRepository,
) {

    data class ProductData(
        val productName: String,
        val productPage: String,
        val productImage: String,
        val productPrice: Int?,
        val productText: String
    )

    // 노트북 페이지 로드
    fun loadLaptopPage() {
        webDriver.get("https://prod.danawa.com/list/?cate=112758")
        println("페이지 타이틀: ${webDriver.title}")
    }

    // 필터 설정
    fun setupFilters() {
        clickOptionButton()
        clickCpuCodeButton()
        selectCpuAttributes()
        Thread.sleep(5000)
    }

    // 제품 리스트 가져오기
    fun fetchProductList(): List<WebElement> {
        scrollToBottom()
        Thread.sleep(5000)
        return getProductList()
    }

    fun extractProductData(product: WebElement): ProductData {
        val name: String = waitForElementToBePresent(product, ".prod_name > a").text
        val detailPage: String = waitForElementToBePresent(product, ".prod_name > a").getAttribute("href")
        val image: String = getImageFromProduct(product)
        val price: Int? =
            try {
                waitForElementToBePresent(product, ".prod_pricelist > ul > li:first-of-type > p.price_sect > a")
                    .text.replace(",", "").replace("원", "").toInt()
            } catch(e: Exception) {
                null
            }

        val text: String = waitForElementToBePresent(product, ".spec_list").text

        return ProductData(
            productName = name,
            productPage = detailPage,
            productImage = image,
            productPrice = price,
            productText = text
        )
    }

    // 제품 상세 정보 파싱
    fun parseProductDetails(data: ProductData): Laptop? {
        try {
            val parsedDetails = extractProductDetails(data.productText)
            val laptop =
            Laptop(
                name = data.productName,
                detailPage = data.productPage,
                imageUrl = data.productImage,
                price = data.productPrice,
                cpuManufacturer = parsedDetails["cpuManufacturer"] as? String,
                cpu = parsedDetails["cpu"] as? String,
                os = parsedDetails["os"] as? String,
                screenSize = parsedDetails["screenSize"] as? Int,
                resolution = parsedDetails["resolution"] as? String,
                brightness = parsedDetails["brightness"] as? Int,
                refreshRate = parsedDetails["refreshRate"] as? Int,
                ramSize = parsedDetails["ramSize"] as? Int,
                ramType = parsedDetails["ramType"] as? String,
                isRamReplaceable = parsedDetails["isRamReplaceable"] as? Boolean,
                graphicsType = parsedDetails["graphicsType"] as? String,
                tgp = parsedDetails["tgp"] as? Int,
                thunderboltCount = parsedDetails["thunderboltCount"] as? Int,
                usbCCount = parsedDetails["usbCCount"] as? Int,
                usbACount = parsedDetails["usbACount"] as? Int,
                sdCard = parsedDetails["sdCard"] as? String,
                isSupportsPdCharging = parsedDetails["isSupportsPdCharging"] as? Boolean,
                batteryCapacity = parsedDetails["batteryCapacity"] as? Double,
                storageCapacity = parsedDetails["storageCapacity"] as? Int,
                storageSlotCount = parsedDetails["storageSlotCount"] as? Int,
                weight = parsedDetails["weight"] as? Double,
                laptopUsage = listOf() // 빈 리스트로 초기화
            )

            val usageList = (parsedDetails["usage"] as? List<String>)?.map { usage ->
                LaptopUsage(usage = usage, laptop = laptop) // 생성 시 즉시 설정
            } ?: emptyList()

            // `NewLaptop`의 `laptopUsage`를 설정
            laptop.laptopUsage = usageList

            return laptop

        } catch (e: Exception) {
            println("제품 파싱 중 오류 발생: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    // 다음 페이지로 이동
    fun navigateToNextPage(): Boolean {
        return try {
            getNextPage()
            Thread.sleep(5000)
            true
        } catch (e: Exception) {
            println("마지막 페이지에 도달하여 크롤링을 종료합니다.")
            false
        }
    }

    // 랩탑 트랙잭션 단위로 저장
    @Transactional
    fun saveOrUpdateLaptop(laptop: Laptop) {
        try {
            val existingLaptop = laptopRepository.findByName(laptop.name)
            if (existingLaptop != null) {
                existingLaptop.price = laptop.price
            } else {
                laptopRepository.save(laptop)
            }
        } catch (e: Exception) {
            println("데이터베이스 작업 중 오류 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    // 내부 사용 메서드들 (private)

    private fun clickOptionButton() {
        val optionButton = waitForElementToBeClickable("#searchOptionAll")
        optionButton.click()
        println("옵션 클릭 완료")
    }

    private fun clickCpuCodeButton() {
        val cpuCodeButton = waitForElementToBeClickable(
            "#extendSearchOptionpriceCompare > div > dl:nth-child(24) > " +
                    "dd > div > button.btn_spec_view.btn_view_more"
        )
        cpuCodeButton.click()
        println("CPU 코드명 클릭 완료")
    }

    private fun selectCpuAttributes() {
        val attributeValues = listOf(
            "#searchAttributeValue984997", // 루나레이크
            "#searchAttributeValue987658", // 스트릭스 포인트
            "#searchAttributeValue929143", // 호크 포인트
            "#searchAttributeValue938068", // 랩터 R
            "#searchAttributeValue928801", // 메테오레이크
            "#searchAttributeValue823528", // 드래곤 레인지
            "#searchAttributeValue823516", // 피닉스
            "#searchAttributeValue823300", // 랩터레이크
            "#searchAttributeValue823525", // 램브란트 R
            "#searchAttributeValue823525", // 램브란트
            "#searchAttributeValue758008", // 엘더레이크
            "#searchAttributeValue845353", // 엘더레이크 N
            "#searchAttributeValue984553", // 오라이온
            "#searchAttributeValue823522", // 바르셀로 R
            "#searchAttributeValue762679", // 바르셀로
            "#searchAttributeValue713962", // 루시엔
            "#searchAttributeValue713959"  // 세잔
        )
        for (attributeValue in attributeValues) {
            val attributeButton = waitForElementToBeClickable(attributeValue)
            attributeButton.click()
        }
        println("CPU 코드 선택 완료")
    }

    private fun scrollToBottom() {
        val jsExecutor = webDriver as JavascriptExecutor
        jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);")
    }

    private fun getProductList(): List<WebElement> {
        val cssSelector = "div.main_prodlist.main_prodlist_list > ul > li .prod_main_info"
        return waitForElementsToBePresent(cssSelector)
    }

    private fun getImageFromProduct(product: WebElement): String {
        val imgElement = product.findElement(By.cssSelector(".thumb_image > a > img"))
        var productImage = imgElement.getAttribute("src")

        // Lazy loading 처리
        if (productImage.contains("noImg")) {
            val dataSrc = imgElement.getAttribute("data-original")
            if (!dataSrc.isNullOrEmpty()) {
                productImage = dataSrc
            }
        }

        // 이미지 src에 https 추가
        if (productImage.startsWith("//")) {
            productImage = "https:$productImage"
        }

        // 이미지 크기 조정
        productImage = productImage.replace("shrink=130:130", "shrink=500:500")

        return productImage
    }

    private fun extractProductDetails(productText: String): Map<String, Any?> {
        val detailsMap = mutableMapOf<String, Any?>()

        // **CPU 정보 추출**
        val (cpuManufacturer, cpuModel) = extractCpuInfo(productText)
        detailsMap["cpuManufacturer"] = cpuManufacturer
        detailsMap["cpu"] = cpuModel

        // **나머지 정보 추출**
        val patterns = mapOf(
            "os" to "운영체제\\(OS\\):\\s*([^/]+)",
            "screenSize" to "화면정보\\s*[0-9.]+cm\\(([0-9.]+)인치\\)",
            "resolution" to "화면정보.*?([0-9]+)x([0-9]+)",
            "brightness" to "화면정보.*?(\\d+)nit",
            "refreshRate" to "(\\d+)\\s*Hz",
            "ramSize" to "램 용량:\\s*(\\d+)GB",
            "ramType" to "램\\s([A-Z0-9]+)",
            "isRamReplaceable" to "램 교체:\\s*(가능|불가능)",
            "graphicsType" to "그래픽\\s[^/]+/([^/]+)",
            "tgp" to "그래픽.*?TGP:\\s*(\\d+)W",
            "thunderboltCount" to "단자.*?썬더볼트\\d+:\\s*(\\d+)개",
            "usbCCount" to "단자.*?USB-C(?::\\s*(\\d+)개(?!\\(USB-C겸용\\))|\\(USB-C겸용\\)|겸용)",
            "usbACount" to "단자.*?USB-A:\\s*(\\d+)개",
            "sdCard" to "단자.*?(SD카드|MicroSD카드)",
            "isSupportsPdCharging" to "전원:\\s*.*USB-PD",
            "batteryCapacity" to "배터리:\\s*(\\d+(\\.\\d+)?)Wh",
            "storageCapacity" to "저장장치.*?/([0-9.]+)([A-Z]+)?/",
            "storageSlotCount" to "저장 슬롯:\\s*(\\d+)개",
            "weight" to "무게:\\s*([0-9.]+)kg",
            "usage" to "용도:\\s*([^/]+)",
        )

        // **패턴 매칭 및 데이터 추출**
        for ((part, pattern) in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(productText)
            if (match != null) {
                val value: Any? = when (part) {
                    "os" -> {
                        val osValue = match.groupValues[1].trim()
                        if (osValue.contains("미포함")) "freedos" else osValue
                    }
                    "usage" -> match.groupValues[1].split(',').map { it.trim() }
                    "isRamReplaceable" -> match.groupValues[1] == "가능"
                    "tgp" -> match.groupValues[1].trim().toIntOrNull() ?: 0
                    "isSupportsPdCharging" -> true
                    "usbCCount" -> {
                        val usbCValue = match.groupValues[1]
                        if (usbCValue.isNotEmpty()) usbCValue.toIntOrNull() ?: 0 else {
                            if (match.value.contains("USB-C")) 1 else 0
                        }
                    }
                    "storageCapacity" -> {
                        val capacity = match.groupValues[1]
                        val unit = match.groupValues[2] ?: "GB"
                        if (unit == "TB") {
                            (capacity.toDouble() * 1024).toInt()
                        } else {
                            capacity.toIntOrNull() ?: 0
                        }
                    }
                    "resolution" -> {
                        val width = match.groupValues[1].toInt()
                        val height = match.groupValues[2].toInt()
//                        width * height
                        "$width x $height"
                    }
                    "refreshRate" -> match.groupValues[1].trim().toIntOrNull() ?: 60
                    "ramSize" -> match.groupValues[1].trim().toIntOrNull()
                    "thunderboltCount" -> match.groupValues[

                        1].trim().toIntOrNull() ?: 0
                    "usbACount" -> match.groupValues[1].trim().toIntOrNull() ?: 0
                    "storageSlotCount" -> match.groupValues[1].trim().toIntOrNull()
                    "brightness" -> match.groupValues[1].trim().toIntOrNull()
                    "weight" -> match.groupValues[1].trim().toDoubleOrNull()
                    "screenSize" -> match.groupValues[1].trim().toDoubleOrNull()?.let { kotlin.math.floor(it).toInt() }
                    "batteryCapacity" -> match.groupValues[1].trim().toDoubleOrNull()
                    else -> match.groupValues[1].trim()
                }
                detailsMap[part] = value
            } else {
                when (part) {
                    "tgp" -> {
                        val graphicsMatch = Regex("그래픽\\s([^/]+)").find(productText)
                        detailsMap["tgp"] = if (graphicsMatch != null && graphicsMatch.groupValues[1].trim() == "내장그래픽") 0 else null
                    }
                    "isSupportsPdCharging" -> detailsMap[part] = false
                    "usbCCount" -> detailsMap[part] = 0
                    "thunderboltCount" -> detailsMap[part] = 0
                    "refreshRate" -> detailsMap[part] = 60
                    "storageSlotCount" -> detailsMap[part] = null
                }
            }
        }

        return detailsMap
    }

    private fun extractCpuInfo(productText: String): Pair<String, String> {
        val sortedCpuModels = cpuModelMap.keys.sortedByDescending { it.length }

        var cpuModel = ""
        var cpuManufacturer = ""

        for (model in sortedCpuModels) {
            val pattern = "\\b${Regex.escape(model)}\\b"
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(productText)) {
                cpuModel = model
                cpuManufacturer = cpuModelMap[model] ?: ""
                break
            }
        }

        return Pair(cpuManufacturer, cpuModel)
    }

    private fun getNextPage() {
        val currentPage: Int = saveCurrentPage()
        println("현재 페이지: $currentPage")
        if (currentPage % 10 == 0) {
            waitForElementToBeClickable("#productListArea > div.prod_num_nav > div > a.edge_nav.nav_next").click()
        } else {
            val nextButtonIndex: Int = currentPage % 10 + 1
            getNextPageButtonByCurrentPage(nextButtonIndex).click()
            println("다음 페이지로 이동")
        }
    }

    private fun saveCurrentPage(): Int {
        return webDriver.findElement(By.cssSelector(".num.now_on")).text.toInt()
    }

    private fun getNextPageButtonByCurrentPage(nextButtonIndex: Int): WebElement {
        val nextPageCssSelector = "#productListArea > div.prod_num_nav > div > div > a:nth-child($nextButtonIndex)"
        return waitForElementToBeClickable(nextPageCssSelector)
    }

    private fun waitForElementToBeClickable(cssSelector: String, timeoutInSeconds: Long = 10): WebElement {
        return WebDriverWait(webDriver, Duration.ofSeconds(timeoutInSeconds)).until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector))
        )
    }

    private fun waitForElementsToBePresent(cssSelector: String): List<WebElement> {
        val wait = WebDriverWait(webDriver, Duration.ofSeconds(20))
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(cssSelector)))
    }

    private fun waitForElementToBePresent(parentElement: WebElement, cssSelector: String): WebElement {
        val wait = WebDriverWait(webDriver, Duration.ofSeconds(20))
        return wait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(parentElement, By.cssSelector(cssSelector)))
    }
}
