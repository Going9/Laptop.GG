package going9.laptopgg.service.crawler

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

// CpuModelMap 파일에서 cpuModelMap을 import
import going9.laptopgg.service.crawler.CpuModelMap.cpuModelMap
import org.openqa.selenium.StaleElementReferenceException

@Service
class CrawlerService(
    val webDriver: WebDriver,
) {

    fun crawlLaptops() {
        // 페이지 로드 및 초기 작업 수행
        loadLaptopPage()
        clickOptionButton()
        clickCpuCodeButton()
        selectCpuAttributes()
        Thread.sleep(10000)



        while (true) {
            // 상세 페이지 링크 획득
            val products = getProductList()

            // 제품 크롤링 확인
            for (product in products) {
                val productName = product.findElement(By.cssSelector(".prod_name > a")).text
                println(productName)
            val parsedDetails = parseProductDetails(product.findElement(By.cssSelector(".spec_list")).text)
            for ((part, detail) in parsedDetails) {
                println("$part: $detail")
            }
                println()
            }

            // 다음 페이지 클릭, 마지막 페이지일 경우 종료
            try {
                getNextPage()
                Thread.sleep(5000) // 페이지 로딩 대기
            } catch (e: Exception) {
                println(e)
                println("마지막 페이지에 도달하여 크롤링을 종료합니다.")
                break // while 루프 종료
            }
        }



    }


    private fun loadLaptopPage() {
        webDriver.get("https://prod.danawa.com/list/?cate=112758")
        println("페이지 타이틀: ${webDriver.title}")
    }

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

    private fun waitForElementToBeClickable(cssSelector: String, timeoutInSeconds: Long = 10): WebElement {
        return WebDriverWait(webDriver, Duration.ofSeconds(timeoutInSeconds)).until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector))
        )
    }

    // 지정된 CSS 선택자를 통해 요소가 나타날 때까지 대기하는 메서드
    private fun waitForElementToBePresent(cssSelector: String): WebElement {
        val wait = WebDriverWait(webDriver, Duration.ofSeconds(20))
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)))
    }

    // 다수의 요소를 기다리는 메서드
    private fun waitForElementsToBePresent(cssSelector: String): List<WebElement> {
        val wait = WebDriverWait(webDriver, Duration.ofSeconds(20))
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(cssSelector)))
    }

    private fun saveCurrentPage(): Int {
        return webDriver.findElement(By.cssSelector(".num.now_on")).text.toInt()
    }

    // 제품 리스트 가져오기 메서드
    private fun getProductList(): List<WebElement> {
        val productElements = mutableListOf<WebElement>()
        val cssSelector = ".product_list .prod_item.prod_layer:not(.product-pot) .prod_main_info .prod_info"

        try {
            // 대기 후 요소를 가져와 리스트에 추가
            productElements.addAll(
                waitForElementsToBePresent(cssSelector)
            )
        } catch (e: StaleElementReferenceException) {
            println("요소가 DOM에서 사라졌습니다. 재시도 중...")
            productElements.clear() // 예외 발생 시 리스트 초기화 후 재시도
            productElements.addAll(
                waitForElementsToBePresent(cssSelector)
            )
        }

        return productElements
    }

    private fun parseProductDetails(productText: String): Map<String, Any> {
        val detailsMap = mutableMapOf<String, Any>()

        // **CPU 정보 추출**
        val (cpuManufacturer, cpuModel) = extractCpuInfo(productText)
        detailsMap["cpuManufacturer"] = cpuManufacturer
        detailsMap["cpu"] = cpuModel

        // **나머지 정보 추출**
        val patterns = mapOf(
            "os" to "운영체제\\(OS\\):\\s*([^/]+)",
            "usage" to "용도:\\s*([^/]+)",
            "screenSize" to "화면정보\\s*[0-9.]+cm\\(([0-9.]+)인치\\)",
            "resolution" to "화면정보.*?/.*?\\(([^)]+)\\)",
            "brightness" to "화면정보.*?(\\d+)nit",
            "refreshRate" to "주사율:\\s*(\\d+)Hz",
            "ramType" to "램\\s([A-Z0-9]+)",
            "ramSize" to "램 용량:\\s*(\\d+)GB",
            "ramClock" to "램 용량:.*?/(\\d+)MHz",
            "ramSlot" to "램 교체:\\s*(가능|불가능)",
            "graphicsType" to "그래픽\\s[^/]+/([^/]+)",
            "tgp" to "그래픽.*?TGP:\\s*(\\d+)W",
            "videoOutput" to "영상입출력\\s([^/]+)",
            "thunderbolt" to "단자.*?썬더볼트\\d+:\\s*(\\d+)개",
            "usbC" to "단자.*?USB-C(?::\\s*(\\d+)개(?!\\(USB-C겸용\\))|\\(USB-C겸용\\)|겸용)",
            "usbA" to "단자.*?USB-A:\\s*(\\d+)개",
            "sdCard" to "단자.*?(SD카드|MicroSD카드)",
            "battery" to "배터리:\\s*(\\d+)Wh",
            "pdCharging" to "전원:\\s*.*USB-PD",
            "storageCapacity" to "저장장치.*?/([0-9.]+)([A-Z]+)?/",
            "storageSlot" to "저장 슬롯:\\s*(\\d+)개",
            "weight" to "무게:\\s*([0-9.]+)kg"
        )

        // **패턴 매칭 및 데이터 추출**
        for ((part, pattern) in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(productText)
            if (match != null) {
                val value = when (part) {
                    "os" -> {
                        val osValue = match.groupValues[1].trim()
                        if (osValue.contains("미포함")) "freedos" else osValue
                    }
                    "usage" -> {
                        match.groupValues[1].split(',').map { it.trim() }
                    }
                    "ramSlot" -> match.groupValues[1] == "가능"
                    "tgp" -> match.groupValues[1]
                    "pdCharging" -> true
                    "usbC" -> {
                        val usbCValue = match.groupValues[1]
                        if (usbCValue.isNotEmpty()) usbCValue else {
                            if (match.value.contains("USB-C")) "1" else "0"
                        }
                    }
                    "storageCapacity" -> {
                        val capacity = match.groupValues[1]
                        val unit = match.groupValues[2] ?: "GB"
                        if (unit == "TB") {
                            (capacity.toDouble() * 1024).toInt().toString()
                        } else {
                            capacity
                        }
                    }
                    else -> match.groupValues[1].trim()
                }
                detailsMap[part] = value
            } else {
                when (part) {
                    "tgp" -> {
                        val graphicsMatch = Regex("그래픽\\s([^/]+)").find(productText)
                        if (graphicsMatch != null && graphicsMatch.groupValues[1].trim() == "내장그래픽") {
                            detailsMap["tgp"] = "0"
                        }
                    }
                    "pdCharging" -> detailsMap[part] = false
                    "usbC" -> detailsMap[part] = "0"
                    "thunderbolt" -> detailsMap[part] = "0"
                }
            }
        }

        return detailsMap
    }

    private fun extractCpuInfo(productText: String): Pair<String, String> {
        // 모델명 길이 순으로 정렬 (긴 것부터)
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
        var currentPage: Int = saveCurrentPage()
        println("현재페이지" + currentPage)
        if (currentPage % 10 == 0) {
            try {
                waitForElementToBeClickable("#productListArea > div.prod_num_nav > div > a.edge_nav.nav_next").click()
            } catch(e: Exception) {
                println(e.message)
                return
            }
        }
        // 10의 배수가 아니면
        else {
            var nextButtonIndex: Int = currentPage % 10 + 1
            getNextPageButtonByCurrentPage(nextButtonIndex).click()
            println("다음페이지 클릭")
        }
    }

    private fun getNextPageButtonByCurrentPage(nextButtonIndex: Int): WebElement {
        val nextPageCssSelector = "#productListArea > div.prod_num_nav > div > div > a:nth-child(${nextButtonIndex})"
        return waitForElementToBeClickable(nextPageCssSelector)
    }

}
