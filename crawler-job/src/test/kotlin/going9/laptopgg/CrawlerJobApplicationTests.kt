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
            "recommendationPageController",
            "laptopPageController",
            "commentPageController",
            "recommendationController",
            "laptopController",
            "commentController",
            "recommendationPageModelFactory",
            "thymeleafViewResolver",
            "commentJpaAdapter",
            "laptopJpaAdapter",
            "laptopProfileJpaAdapter",
            "commentLaptopJpaAdapter",
            "laptopDetailJpaAdapter",
            "recommendationCandidateJpaAdapter",
            "commentRepository",
            "webLaptopRepository",
            "webLaptopProfileRepository",
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

    @Test
    fun `crawler context scans only crawler persistence entities`() {
        val entityTypes = entitySimpleNames()

        assertThat(entityTypes).contains(
            "CrawlerRun",
            "LaptopPriceHistory",
            "Laptop",
            "LaptopProfile",
            "LaptopUsage",
            "RecommendationScore",
        )
        assertThat(entityTypes).doesNotContain(
            "Comment",
        )
    }

    private fun entitySimpleNames(): Set<String> {
        val entityManagerFactory = beanFactory.getBean("entityManagerFactory")
        val metamodel = entityManagerFactory.zeroArgMethod("getMetamodel").invoke(entityManagerFactory)
        val entities = metamodel.zeroArgMethod("getEntities").invoke(metamodel) as Collection<*>

        return entities.mapNotNull { entityType ->
            val javaType = entityType?.zeroArgMethod("getJavaType")?.invoke(entityType) as? Class<*>
            javaType?.simpleName
        }.toSet()
    }

    private fun Any.zeroArgMethod(name: String) = javaClass.methods.first { method ->
        method.name == name && method.parameterCount == 0
    }
}
