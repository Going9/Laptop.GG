package going9.laptopgg.batch

import going9.laptopgg.domain.laptop.NewLaptop
import going9.laptopgg.domain.repository.NewLaptopRepository
import going9.laptopgg.service.crawler.CrawlerService
import org.openqa.selenium.By
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
        crawlerService.clickOptionButton()
        crawlerService.clickCpuCodeButton()
        crawlerService.selectCpuAttributes()
        Thread.sleep(5000)

        // 크롤링 루프 시작
        while (true) {
            Thread.sleep(30000)
            crawlerService.scrollToBottom()
            Thread.sleep(10000)

            // 현재 페이지의 제품 리스트 가져오기
            val products = crawlerService.getProductList()

            // 제품 정보 파싱 및 엔티티 생성
            val laptops = mutableListOf<NewLaptop>()
            for (product in products) {
                try {
                    val productName = product.findElement(By.cssSelector(".prod_name > a")).text
                    val productImage = crawlerService.getImageFromProduct(product)
                    val productPrice =
                        product.findElement(By.cssSelector(".prod_pricelist > ul > li.rank_one > p.price_sect > a")).text
                        .replace(",", "") // 쉼표 제거
                        .replace("원", "") // "원" 제거
                        .toInt() // Int로 변환
                    val productText = product.findElement(By.cssSelector(".spec_list")).text
                    val parsedDetails = crawlerService.parseProductDetails(productText)

                    val existingLaptop = newLaptopRepository.findByName(productName)

                    if (existingLaptop != null) {
                        // db에 이미 있는 노트북이라면 가격만 업데이트
                        existingLaptop.price = productPrice
                        newLaptopRepository.save(existingLaptop)
                    } else {
                        // db에 없는 노트북이면 새로 저장
                        val newLaptop = NewLaptop(
                            name = productName,
                            imageUrl = productImage,
                            price = productPrice,
                            cpuManufacturer = parsedDetails["cpuManufacturer"] as String,
                            cpu = parsedDetails["cpu"] as String,
                            os = parsedDetails["os"] as String,
                            screenSize = (parsedDetails["screenSize"] as String).toDouble(),
                            resolution = parsedDetails["resolution"] as String,
                            brightness = (parsedDetails["brightness"] as String).toInt(),
                            refreshRate = (parsedDetails["refreshRate"] as String).toInt(),
                            ramSize = (parsedDetails["ramSize"] as String).toInt(),
                            ramType = parsedDetails["ramType"] as String,
                            ramClock = (parsedDetails["ramClock"] as String).toInt(),
                            isRamReplaceable = parsedDetails["isRamReplaceable"] as Boolean,
                            graphicsType = parsedDetails["graphicsType"] as String,
                            tgp = (parsedDetails["tgp"] as String).toInt(),
                            videoOutput = parsedDetails["videoOutput"] as String,
                            thunderboltCount = (parsedDetails["thunderboltCount"] as String).toInt(),
                            usbCCount = (parsedDetails["usbCCount"] as String).toInt(),
                            usbACount = (parsedDetails["usbACount"] as String).toInt(),
                            sdCard = parsedDetails["sdCard"] as String,
                            isSupportsPdCharging = parsedDetails["isSupportsPdCharging"] as Boolean,
                            batteryCapacity = (parsedDetails["batteryCapacity"] as String).toInt(),
                            storageCapacity = (parsedDetails["storageCapacity"] as String).toInt(),
                            storageSlotCount = (parsedDetails["storageSlotCount"] as String).toInt(),
                            weight = (parsedDetails["weight"] as String).toDouble(),
                            usage = parsedDetails["usage"] as List<String>
                        )
                        laptops.add(newLaptop)
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
                crawlerService.getNextPage()
                Thread.sleep(5000) // 페이지 로딩 대기
            } catch (e: Exception) {
                println("마지막 페이지에 도달하여 크롤링을 종료합니다.")
                break
            }
        }

        return RepeatStatus.FINISHED
    }
}