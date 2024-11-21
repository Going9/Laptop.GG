package going9.laptopgg.controller.crawler

import going9.laptopgg.service.crawler.CrawlerService
import org.springframework.batch.core.Job
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/crawl")
class CrawlerController(
    private val jobLauncher: JobLauncher,
    private val laptopJob: Job,
    private val crawlerService: CrawlerService,
) {

    @GetMapping("/laptops")
    fun startCrawling() {
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
}