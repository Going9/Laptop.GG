package going9.laptopgg.service.crawler

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CrawlerService(
    val webDriver: WebDriver,
) {

    fun crawlLaptops() {
        // 페이지 로드 및 초기 작업 수행
        loadLaptopPage()

        // 옵션 전체 보기 클릭
        //clickOptionButton()

        // cpu 전체보기 클릭
        //clickCpuCodeButton()

        // 특정 cpu 선택
        //selectCpuAttributes()

        // 상세 페이지 링크 획득
        // 상세 페이지 링크 획득
        val products = getProductLinkList()

        for (product in products) {
            println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
            //println(product.findElement(By.cssSelector(".prod_info > p > a")).text)  // 이름
            //println(product.findElement(By.cssSelector(".prod_info > div")).text)  // 스펙
            val parsedDetails = parseProductDetails(product.findElement(By.cssSelector(".prod_info > div")).text)
            for ((part, detail) in parsedDetails) {
                println("$part: $detail")
            }
            println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
        }
        println("크롤링 완료")
    }

    private fun loadLaptopPage() {
        webDriver.get("https://prod.danawa.com/list/?cate=112758")
        println("페이지 타이틀: ${webDriver.title}")
    }

    private fun clickOptionButton() {
        val optionButton = waitForElement("#searchOptionAll")
        optionButton.click()
        println("옵션 클릭 완료")
    }

    private fun clickCpuCodeButton() {
        val cpuCodeButton = waitForElement("#extendSearchOptionpriceCompare > div > dl:nth-child(24) > " +
                "dd > div > button.btn_spec_view.btn_view_more")
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
            val attributeButton = waitForElement(attributeValue)
            println(attributeButton.text)
            attributeButton.click()
        }
        println("CPU 코드 선택 완료")
    }

    private fun waitForElement(cssSelector: String, timeoutInSeconds: Long = 10): WebElement {
        return WebDriverWait(webDriver, Duration.ofSeconds(timeoutInSeconds)).until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector))
        )
    }

    private fun saveCurrentPage(): Int {
        return webDriver.findElement(By.cssSelector(".num.now_on")).text.toInt()
    }

    fun getProductLinkList(): List<WebElement> {
        val wait = WebDriverWait(webDriver, Duration.ofSeconds(20))
        val productList = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".main_prodlist.main_prodlist_list > .product_list .prod_item.prod_layer:not(.product-pot)")
            )
        )
        return productList
    }

    fun parseProductDetails(productText: String): Map<String, String> {
        val detailsMap = mutableMapOf<String, String>()

        // 정규표현식을 사용하여 각 부품 섹션을 추출
        val patterns = mapOf(
            "운영체제" to "운영체제\\(OS\\):\\s*([^/]+)",
            "용도" to "용도:\\s*([^/]+)",
            "화면정보" to "화면정보\\s*([^/]+)",
            "CPU" to "CPU\\s*([^/]+)",
            "램" to "램\\s*([^/]+)",
            "그래픽" to "그래픽\\s*([^/]+)",
            "저장장치" to "저장장치\\s*([^/]+)",
            "네트워크" to "네트워크\\s*([^/]+)",
            "영상입출력" to "영상입출력\\s*([^/]+)",
            "단자" to "단자\\s*([^/]+)",
            "파워" to "파워\\s*([^/]+)",
            "주요제원" to "주요제원\\s*([^/]+)"
        )

        // 각 패턴에 대해 텍스트를 검색하고 매칭된 결과를 Map에 추가
        for ((part, pattern) in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(productText)
            if (match != null) {
                detailsMap[part] = match.groupValues[1].trim()
            }
        }

        return detailsMap
    }
}