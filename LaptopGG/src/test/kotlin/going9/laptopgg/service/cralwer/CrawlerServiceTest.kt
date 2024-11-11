package going9.laptopgg.service.cralwer

import going9.laptopgg.service.crawler.CrawlerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.WebDriver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(properties = ["spring.profiles.active=test"])
class CrawlerServiceTest {

    @Autowired
    private lateinit var crawlerService: CrawlerService

    @Autowired
    private lateinit var driver: WebDriver

    @Test
    fun `test initCrawler with actual browser`() {
        crawlerService.crawlLaptops()

    }

    @AfterEach
    fun tearDown() {
        driver.quit()
    }
}