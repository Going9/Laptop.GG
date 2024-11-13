package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.NewLaptop
import going9.laptopgg.domain.repository.NewLaptopRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.Executors

@SpringBootTest(properties = ["spring.profiles.active=test"])
class CrawlerServiceTest(
) {
    @Autowired
    private lateinit var newLaptopRepository: NewLaptopRepository

    @Autowired
    private lateinit var crawlerService: CrawlerService

    @Autowired
    private lateinit var driver: WebDriver

    @Test
    fun `test fetchProductList retrieves product list`() {
        var executor = Executors.newVirtualThreadPerTaskExecutor()

        try {
            // 초기 작업 수행
            crawlerService.loadLaptopPage()
            crawlerService.setupFilters()
            Thread.sleep(5000)

            // 크롤링 루프 시작
            while (true) {
                // 제품 리스트 가져오기
                val products = crawlerService.fetchProductList()

                // 제품 정보 파싱 및 엔티티 생성
                val futures = products.map { product ->
                    executor.submit<NewLaptop?> {
                        val newLaptop = crawlerService.parseProductDetails(product)
                        if (newLaptop != null) {
                            val existingLaptop = newLaptopRepository.findByName(newLaptop.name)
                            if (existingLaptop != null) {
                                existingLaptop.price = newLaptop.price
                                newLaptopRepository.save(existingLaptop)
                                println(executor.toString())
                                null
                            } else {
                                newLaptop
                            }
                        } else {
                            null
                        }
                    }
                }

                val laptopsToSave = futures.mapNotNull { it.get() }

                if (laptopsToSave.isNotEmpty()) {
                    println("새로운 노트북 정보를 저장합니다.")
                    newLaptopRepository.saveAll(laptopsToSave)
                }

                // 다음 페이지로 이동, 마지막 페이지 도달 시 종료
                if (!crawlerService.navigateToNextPage()) break
            }
        } finally {
            executor.shutdown()
        }
    }



//            val laptopsToSave = mutableListOf<NewLaptop>()
//            for (product in products) {
//                val newLaptop = crawlerService.parseProductDetails(product)
//                newLaptop?.let { laptop ->
//                    val existingLaptop = newLaptopRepository.findByName(laptop.name)
//                    if (existingLaptop != null) {
//                        // 이미 존재하는 경우 가격만 업데이트
//                        existingLaptop.price = laptop.price
//                        newLaptopRepository.save(existingLaptop)
//                    } else {
//                        laptopsToSave.add(laptop)
//                    }
//                }
//            }

//            // 데이터베이스에 새로운 랩탑 정보 저장
//            if (laptopsToSave.isNotEmpty()) {
//                println("새로운 노트북 정보를 저장합니다.")
//                newLaptopRepository.saveAll(laptopsToSave)
//            }
//
//            // 다음 페이지로 이동, 마지막 페이지 도달 시 종료
////            if (!crawlerService.navigateToNextPage()) break
//        }
//    }

    @AfterEach
    fun tearDown() {
        driver.quit()
    }
}
