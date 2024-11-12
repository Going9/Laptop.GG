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

            // 제품 정보 파싱 및 엔티티 생성
            val laptopsToSave = mutableListOf<NewLaptop>()
            for (product in products) {
                val newLaptop = crawlerService.parseProductDetails(product)
                newLaptop?.let { laptop ->
                    val existingLaptop = newLaptopRepository.findByName(laptop.name)
                    if (existingLaptop != null) {
                        // 이미 존재하는 경우 가격만 업데이트
                        existingLaptop.price = laptop.price
                        newLaptopRepository.save(existingLaptop)
                    } else {
                        laptopsToSave.add(laptop)
                    }
                }
            }

            // 데이터베이스에 새로운 랩탑 정보 저장
            if (laptopsToSave.isNotEmpty()) {
                println("새로운 노트북 정보를 저장합니다.")
                newLaptopRepository.saveAll(laptopsToSave)
            }

            // 다음 페이지로 이동, 마지막 페이지 도달 시 종료
            if (!crawlerService.navigateToNextPage()) break
        }

        return RepeatStatus.FINISHED
    }
}
