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

        return RepeatStatus.FINISHED
    }
}
