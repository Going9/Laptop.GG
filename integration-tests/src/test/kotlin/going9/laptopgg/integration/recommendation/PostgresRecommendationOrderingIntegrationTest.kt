package going9.laptopgg.integration.recommendation

import going9.laptopgg.InfrastructureJpaTestApplication
import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.integration.recommendation.support.RecommendationIntegrationFixtures
import going9.laptopgg.integration.support.PostgresIntegrationDatabase
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@EnabledIfEnvironmentVariable(named = "POSTGRES_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(
    classes = [InfrastructureJpaTestApplication::class],
    properties = [
        "spring.profiles.active=postgres,crawler",
        "spring.config.import=classpath:/laptopgg-persistence.yml",
        "spring.flyway.clean-disabled=false",
        "spring.datasource.hikari.maximum-pool-size=2",
    ],
)
class PostgresRecommendationOrderingIntegrationTest {
    @Autowired
    lateinit var flyway: Flyway

    @Autowired
    lateinit var recommendLaptopsUseCase: RecommendLaptopsUseCase

    @Autowired
    lateinit var laptopRepository: CrawlerLaptopRepository

    @Autowired
    lateinit var laptopProfileRepository: CrawlerLaptopProfileRepository

    @Autowired
    lateinit var saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase

    @Autowired
    lateinit var recommendationScorePort: RecommendationScorePort

    @Autowired
    lateinit var crawlerTransactionPort: CrawlerTransactionPort

    private lateinit var fixtures: RecommendationIntegrationFixtures

    @BeforeEach
    fun resetDatabase() {
        flyway.clean()
        flyway.migrate()
        fixtures = RecommendationIntegrationFixtures(
            laptopRepository = laptopRepository,
            laptopProfileRepository = laptopProfileRepository,
            saveCrawledLaptopUseCase = saveCrawledLaptopUseCase,
            recommendationScorePort = recommendationScorePort,
            crawlerTransactionPort = crawlerTransactionPort,
        )
    }

    @Test
    fun `recommended database pages match calculator order for every use case on postgres`() {
        val laptops = fixtures.persistSortProbeLaptops()
        fixtures.overrideSortProbeScores(laptops)

        RecommendationUseCase.entries.forEach { useCase ->
            val request = LaptopRecommendationQuery(
                budget = 2_000_000,
                maxWeightKg = 3.0,
                screenSizeMode = ScreenSizeMode.ANY,
                useCase = useCase,
            )
            val actual = listOf(
                recommendLaptopsUseCase.recommend(request, page(0, 2)).content,
                recommendLaptopsUseCase.recommend(request, page(1, 2)).content,
            ).flatten()

            val expectedNames = actual
                .map { response ->
                    CalculatorSortProbe(
                        name = response.name,
                        score = response.score,
                        price = response.price,
                        id = response.id,
                    )
                }
                .sortedWith(
                    compareByDescending<CalculatorSortProbe> { it.score }
                        .thenBy { it.price ?: Int.MAX_VALUE }
                        .thenBy { it.id },
                )
                .map { it.name }

            assertThat(actual.map { it.name })
                .describedAs("postgres recommended order for $useCase")
                .isEqualTo(expectedNames)
        }
    }

    private fun page(page: Int, size: Int): PageQuery {
        return PageQuery(page = page, size = size)
    }

    private data class CalculatorSortProbe(
        val name: String,
        val score: Double,
        val price: Int?,
        val id: Long,
    )

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            PostgresIntegrationDatabase.registerSpringDatasource(registry)
        }
    }
}
