package going9.laptopgg.integration.recommendation.support

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder
import going9.laptopgg.application.common.SortProperty
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.profiles.active=test,crawler"])
@Transactional
abstract class RecommendationIntegrationTestSupport {
    @Autowired
    protected lateinit var recommendLaptopsUseCase: RecommendLaptopsUseCase

    @Autowired
    protected lateinit var laptopRepository: CrawlerLaptopRepository

    @Autowired
    protected lateinit var laptopProfileRepository: CrawlerLaptopProfileRepository

    @Autowired
    protected lateinit var laptopProfileService: LaptopProfileService

    @Autowired
    protected lateinit var recommendationScoreService: RecommendationScoreService

    @Autowired
    protected lateinit var recommendationScoreRepository: RecommendationScoreRepository

    protected lateinit var fixtures: RecommendationIntegrationFixtures

    @BeforeEach
    fun resetRecommendationData() {
        recommendationScoreRepository.deleteAll()
        laptopProfileRepository.deleteAll()
        laptopRepository.deleteAll()
        fixtures = RecommendationIntegrationFixtures(
            laptopRepository = laptopRepository,
            laptopProfileRepository = laptopProfileRepository,
            laptopProfileService = laptopProfileService,
            recommendationScoreService = recommendationScoreService,
        )
    }

    protected fun page(page: Int, size: Int, vararg orders: SortOrder): PageQuery {
        return PageQuery(
            page = page,
            size = size,
            sort = orders.toList(),
        )
    }

    protected fun sortOrder(property: SortProperty, direction: SortDirection): SortOrder {
        return SortOrder(
            property = property,
            direction = direction,
        )
    }
}
