package going9.laptopgg.batch

import going9.laptopgg.domain.laptop.NewLaptop
import going9.laptopgg.domain.repository.NewLaptopRepository
import going9.laptopgg.service.crawler.CrawlerService
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

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

        // 크롤링 루프 시작
        while (true) {
            // 제품 리스트 가져오기
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

            // 다음 페이지로 이동, 마지막 페이지 도달 시 종료
            if (!crawlerService.navigateToNextPage()) break
        }

        return RepeatStatus.FINISHED
    }
}
