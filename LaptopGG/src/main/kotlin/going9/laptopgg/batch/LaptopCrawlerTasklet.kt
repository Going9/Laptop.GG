package going9.laptopgg.batch

import going9.laptopgg.domain.laptop.NewLaptop
import going9.laptopgg.domain.repository.NewLaptopRepository
import going9.laptopgg.service.crawler.CrawlerService
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class LaptopCrawlerTasklet(
    private val crawlerService: CrawlerService,
    private val newLaptopRepository: NewLaptopRepository,
) : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus? {
        // 초기 작업 수행
        crawlerService.loadLaptopPage()
        crawlerService.setupFilters()
        Thread.sleep(5000)

        var executor = Executors.newVirtualThreadPerTaskExecutor()

        try {
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
        } finally {
            executor.shutdown()
        }

        return RepeatStatus.FINISHED
    }
}
