package going9.laptopgg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "app.crawler.run-on-startup=false",
        "spring.main.web-application-type=none",
    ],
)
@ActiveProfiles("test", "crawler")
class CrawlerJobApplicationTests {
    @Autowired
    lateinit var beanFactory: ListableBeanFactory

    @Test
    fun `crawler context does not load web beans`() {
        val beanNames = beanFactory.beanDefinitionNames.toSet()

        assertThat(beanNames).doesNotContain(
            "pageController",
            "recommendationController",
            "thymeleafViewResolver",
            "commentJpaAdapter",
            "laptopJpaAdapter",
            "laptopProfileJpaAdapter",
            "commentRepository",
        )
    }

    @Test
    fun `crawler context does not load user flow use cases`() {
        val beanNames = beanFactory.beanDefinitionNames.toSet()

        assertThat(beanNames).doesNotContain(
            "getLaptopDetailUseCase",
            "manageCommentUseCase",
            "recommendLaptopsUseCase",
            "recommendationScoreCalculator",
        )
    }

    @Test
    fun `crawler context loads crawler application beans`() {
        val beanNames = beanFactory.beanDefinitionNames.toSet()

        assertThat(beanNames).contains(
            "saveCrawledLaptopService",
            "trackCrawlerRunService",
            "crawledCpuModelResolver",
        )
    }
}
