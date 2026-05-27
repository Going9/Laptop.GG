package going9.laptopgg.service.crawler

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@Disabled("실제 다나와 페이지와 브라우저가 필요한 라이브 통합 테스트입니다.")
@SpringBootTest(properties = ["spring.profiles.active=test"])
class CrawlerServiceTest {

    @Test
    fun `live crawling smoke test`() {
    }
}
