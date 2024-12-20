package going9.laptopgg.service.crawler

import going9.laptopgg.domain.repository.LaptopRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.WebDriver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.Executors

@SpringBootTest(properties = ["spring.profiles.active=test"])
class CrawlerServiceTest(
) {
    @Autowired
    private lateinit var laptopRepository: LaptopRepository

    @Autowired
    private lateinit var crawlerService: CrawlerService

    @Autowired
    private lateinit var driver: WebDriver

    @Test
    fun `check crawling time without threading`() {
        // 초기 작업 수행
        crawlerService.loadLaptopPage()
        crawlerService.setupFilters()
        Thread.sleep(5000)

        // 크롤링 루프 시작
        while (true) {
            val startTime = System.currentTimeMillis()

            val products = crawlerService.fetchProductList()

            // 스레드 안정성을 위해서 메인 스레드에서 제품 데이터 추출
            val productDataList = products.map { product ->
                crawlerService.extractProductData(product)
            }

            productDataList.map { productData ->
                val newLaptop = crawlerService.parseProductDetails(productData)
                if (newLaptop != null) {
                    crawlerService.saveOrUpdateLaptop(newLaptop)
                }
            }

            val endTime = System.currentTimeMillis()

            println("걸린시간: " + (endTime - startTime))

            // 다음 페이지로 이동, 마지막 페이지 도달 시 종료
            if (!crawlerService.navigateToNextPage()) break
        }
    }

    @Test
    fun `test crawling with VT`() {
        var executor = Executors.newVirtualThreadPerTaskExecutor()

        // 초기 작업 수행
        crawlerService.loadLaptopPage()
        crawlerService.setupFilters()
        Thread.sleep(5000)

        try {
            // 크롤링 루프 시작
            while (true) {
                val startTime = System.currentTimeMillis()

                // 제품 리스트 가져오기
                val products = crawlerService.fetchProductList()

                val futures = products.map { product ->
                    executor.submit {
                        val productData = crawlerService.extractProductData(product)
                        val newLaptop = crawlerService.parseProductDetails(productData)
                        if (newLaptop != null) {
                            crawlerService.saveOrUpdateLaptop(newLaptop)
                        }
                    }
                }

                // 모든 작업 완료될 때 까지 대기
                futures.forEach { it.get() }

                val endTime = System.currentTimeMillis()

                println("걸린시간: " + (endTime - startTime))

                // 다음 페이지로 이동, 마지막 페이지 도달 시 종료
                if (!crawlerService.navigateToNextPage()) break
            }
        } finally {
            executor.shutdown()
        }
    }

    @AfterEach
    fun tearDown() {
        driver.quit()
    }
}
