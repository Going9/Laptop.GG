package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.NewLaptop
import going9.laptopgg.domain.repository.NewLaptopRepository
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

// CpuModelMap 파일에서 cpuModelMap을 import
import going9.laptopgg.service.crawler.CpuModelMap.cpuModelMap
import org.openqa.selenium.*

@Service
class CrawlerService(
    val webDriver: WebDriver,
    private val newLaptopRepository: NewLaptopRepository,
) {

    fun crawlLaptops() {
        // 페이지 로드 및 초기 작업 수행
        loadLaptopPage()
        clickOptionButton()
        clickCpuCodeButton()
        selectCpuAttributes()
        Thread.sleep(5000)



        while (true) {
            Thread.sleep(5000)
            scrollToBottom()
            Thread.sleep(5000)

            // 현재 페이지의 제품 리스트 가져오기
            val products = getProductList()

            // 제품 정보 파싱 및 엔티티 생성
            val laptops = mutableListOf<NewLaptop>()
            for (product in products) {
                try {
                    val productName = product.findElement(By.cssSelector(".prod_name > a")).text
                    val productImage = getImageFromProduct(product)
                    val productPrice =
                        product.findElement(By.cssSelector(".prod_pricelist > ul > li.rank_one > p.price_sect > a")).text
                            .replace(",", "") // 쉼표 제거
                            .replace("원", "") // "원" 제거
                            .toInt() // Int로 변환
                    val productText = product.findElement(By.cssSelector(".spec_list")).text
                    val parsedDetails = parseProductDetails(productText)

                    val existingLaptop = newLaptopRepository.findByName(productName)

                    if (existingLaptop != null) {
                        // db에 이미 있는 노트북이라면 가격만 업데이트
                        existingLaptop.price = productPrice
                        newLaptopRepository.save(existingLaptop)
                    } else {
                        println(productText)

                        try {
                            val cpuManufacturer = parsedDetails["cpuManufacturer"] as? String
                            val cpu = parsedDetails["cpu"] as? String
                            val os = parsedDetails["os"] as? String
                            val screenSize = (parsedDetails["screenSize"] as? String)?.toDoubleOrNull()
                            val resolution = parsedDetails["resolution"] as? String
                            val brightness = (parsedDetails["brightness"] as? String)?.toIntOrNull()
                            val refreshRate = (parsedDetails["refreshRate"] as? String)?.toIntOrNull()
                            val ramSize = (parsedDetails["ramSize"] as? String)?.toIntOrNull()
                            val ramType = parsedDetails["ramType"] as? String
                            val isRamReplaceable = parsedDetails["isRamReplaceable"] as? Boolean
                            val graphicsType = parsedDetails["graphicsType"] as? String
                            val tgp = (parsedDetails["tgp"] as? String)?.toIntOrNull()
                            val videoOutput = parsedDetails["videoOutput"] as? String
                            val thunderboltCount = (parsedDetails["thunderboltCount"] as? String)?.toIntOrNull()
                            val usbCCount = (parsedDetails["usbCCount"] as? String)?.toIntOrNull()
                            val usbACount = (parsedDetails["usbACount"] as? String)?.toIntOrNull()
                            val sdCard = parsedDetails["sdCard"] as? String
                            val isSupportsPdCharging = parsedDetails["isSupportsPdCharging"] as? Boolean
                            val batteryCapacity = (parsedDetails["batteryCapacity"] as? String)?.toIntOrNull()
                            val storageCapacity = (parsedDetails["storageCapacity"] as? String)?.toIntOrNull()
                            val storageSlotCount = (parsedDetails["storageSlotCount"] as? String)?.toIntOrNull()
                            val weight = (parsedDetails["weight"] as? String)?.toDoubleOrNull()
                            val usage = parsedDetails["usage"] as? List<String>

                            val newLaptop = NewLaptop(
                                name = productName,
                                imageUrl = productImage,
                                price = productPrice,
                                cpuManufacturer = cpuManufacturer,
                                cpu = cpu,
                                os = os,
                                screenSize = screenSize,
                                resolution = resolution,
                                brightness = brightness,
                                refreshRate = refreshRate,
                                ramSize = ramSize,
                                ramType = ramType,
                                isRamReplaceable = isRamReplaceable,
                                graphicsType = graphicsType,
                                tgp = tgp,
                                videoOutput = videoOutput,
                                thunderboltCount = thunderboltCount,
                                usbCCount = usbCCount,
                                usbACount = usbACount,
                                sdCard = sdCard,
                                isSupportsPdCharging = isSupportsPdCharging,
                                batteryCapacity = batteryCapacity,
                                storageCapacity = storageCapacity,
                                storageSlotCount = storageSlotCount,
                                weight = weight,
                                usage = usage
                            )

                            laptops.add(newLaptop)
                            println("노트북 추가완료")
                            println()
                        } catch (e: Exception) {
                            println("제품 파싱 중 오류 발생: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("제품 파싱 중 오류 발생: ${e.message}")
                }
            }

            // 데이터베이스에 저장
            if (laptops.isNotEmpty()) {
                println("랩탑 저장 완료")
                newLaptopRepository.saveAll(laptops)
            }

            // 다음 페이지로 이동
            try {
                getNextPage()
                Thread.sleep(5000) // 페이지 로딩 대기
            } catch (e: Exception) {
                println("마지막 페이지에 도달하여 크롤링을 종료합니다.")
                break
            }
        }



    }


    fun loadLaptopPage() {
        webDriver.get("https://prod.danawa.com/list/?cate=112758")
        println("페이지 타이틀: ${webDriver.title}")
    }

    fun clickOptionButton() {
        val optionButton = waitForElementToBeClickable("#searchOptionAll")
        optionButton.click()
        println("옵션 클릭 완료")
    }

    fun clickCpuCodeButton() {
        val cpuCodeButton = waitForElementToBeClickable(
            "#extendSearchOptionpriceCompare > div > dl:nth-child(24) > " +
                    "dd > div > button.btn_spec_view.btn_view_more"
        )
        cpuCodeButton.click()
        println("CPU 코드명 클릭 완료")
    }

    fun selectCpuAttributes() {
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
    fun getProductList(): List<WebElement> {
        val productElements = mutableListOf<WebElement>()
        val cssSelector = "div.main_prodlist.main_prodlist_list > ul > li .prod_main_info"

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

    fun parseProductDetails(productText: String): Map<String, Any> {
        val detailsMap = mutableMapOf<String, Any>()

        // **CPU 정보 추출**
        val (cpuManufacturer, cpuModel) = extractCpuInfo(productText)
        detailsMap["cpuManufacturer"] = cpuManufacturer
        detailsMap["cpu"] = cpuModel

        // **나머지 정보 추출**
        val patterns = mapOf(
            "os" to "운영체제\\(OS\\):\\s*([^/]+)",
            "screenSize" to "화면정보\\s*[0-9.]+cm\\(([0-9.]+)인치\\)",
            "resolution" to "화면정보.*?/.*?\\(([^)]+)\\)",
            "brightness" to "화면정보.*?(\\d+)nit",
            "refreshRate" to "주사율:\\s*(\\d+)Hz",
            "ramSize" to "램 용량:\\s*(\\d+)GB",
            "ramType" to "램\\s([A-Z0-9]+)",
            "isRamReplaceable" to "램 교체:\\s*(가능|불가능)",
            "graphicsType" to "그래픽\\s[^/]+/([^/]+)",
            "tgp" to "그래픽.*?TGP:\\s*(\\d+)W",
            "videoOutput" to "영상입출력\\s([^/]+)",
            "thunderboltCount" to "단자.*?썬더볼트\\d+:\\s*(\\d+)개",
            "usbCCount" to "단자.*?USB-C(?::\\s*(\\d+)개(?!\\(USB-C겸용\\))|\\(USB-C겸용\\)|겸용)",
            "usbACount" to "단자.*?USB-A:\\s*(\\d+)개",
            "sdCard" to "단자.*?(SD카드|MicroSD카드)",
            "isSupportsPdCharging" to "전원:\\s*.*USB-PD",
            "batteryCapacity" to "배터리:\\s*(\\d+)Wh",
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
                val value = when (part) {
                    "os" -> {
                        val osValue = match.groupValues[1].trim()
                        if (osValue.contains("미포함")) "freedos" else osValue
                    }
                    "usage" -> {
                        match.groupValues[1].split(',').map { it.trim() }
                    }
                    "isRamReplaceable" -> match.groupValues[1] == "가능"
                    "tgp" -> match.groupValues[1]
                    "isSupportsPdCharging" -> true
                    "usbCCount" -> {
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
                    "isSupportsPdCharging" -> detailsMap[part] = false
                    "usbCCount" -> detailsMap[part] = "0"
                    "thunderboltCount" -> detailsMap[part] = "0"
                    "refreshRate" -> detailsMap[part] = 60
                    "storageSlotCount" -> detailsMap[part] = "unknown"
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

    fun getNextPage() {
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

    // 페이지 아래로 스크롤하는 메서드 추가
    fun scrollToBottom() {
        val jsExecutor = webDriver as JavascriptExecutor
        jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);")
    }

    fun getImageFromProduct(product: WebElement): String {
        val imgElement = product.findElement(By.cssSelector(".thumb_image > a > img"))
        var productImage = imgElement.getAttribute("src")

        // lazy laoding 때문에 palceholder 이미지가 아니라 실제 이미지 url을 가져와야함
        if (productImage.contains("noImg")) {
            val dataSrc = imgElement.getAttribute("data-original")
            if (!dataSrc.isNullOrEmpty()) {
                productImage = dataSrc
            }
        }

        // image src에 https 추가
        if (productImage.startsWith("//")) {
            productImage = "https:$productImage"
        }

        // 이미지 URL에서 'shrink' 파라미터를 변경하여 큰 이미지 가져오기
        productImage = productImage.replace("shrink=130:130", "shrink=500:500")

        return productImage
    }

}
