package going9.laptopgg.integration

import going9.laptopgg.InfrastructureJpaTestApplication
import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.crawler.CrawledLaptopCommand
import going9.laptopgg.application.crawler.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.SaveResult
import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.recommendation.RecommendationUseCase
import going9.laptopgg.infrastructure.jpa.repository.LaptopPriceHistoryRepository
import going9.laptopgg.infrastructure.jpa.repository.LaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.LaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.RecommendationScoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

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
class PostgresCrawlerPersistenceUseCaseTest {
    @Autowired
    lateinit var flyway: Flyway

    @Autowired
    lateinit var saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase

    @Autowired
    lateinit var recommendLaptopsUseCase: RecommendLaptopsUseCase

    @Autowired
    lateinit var laptopRepository: LaptopRepository

    @Autowired
    lateinit var laptopProfileRepository: LaptopProfileRepository

    @Autowired
    lateinit var laptopPriceHistoryRepository: LaptopPriceHistoryRepository

    @Autowired
    lateinit var recommendationScoreRepository: RecommendationScoreRepository

    @BeforeEach
    fun resetDatabase() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `crawler save use case writes profile price history scores and recommendation query on postgres`() {
        val created = saveCrawledLaptopUseCase.saveOrUpdateLaptop(crawledLaptop(price = 1_490_000))

        assertThat(created).isEqualTo(SaveResult.CREATED)
        assertThat(laptopRepository.count()).isEqualTo(1)
        assertThat(laptopProfileRepository.count()).isEqualTo(1)
        assertThat(laptopPriceHistoryRepository.findAll().map { it.price }).containsExactly(1_490_000)
        assertThat(recommendationScoreRepository.findAll()).hasSize(RecommendationUseCase.entries.size)

        val updated = saveCrawledLaptopUseCase.saveOrUpdateLaptop(
            crawledLaptop(
                name = "Postgres Verified 14",
                price = 1_390_000,
            ),
        )

        assertThat(updated).isEqualTo(SaveResult.UPDATED)
        assertThat(laptopRepository.count()).isEqualTo(1)
        assertThat(laptopPriceHistoryRepository.findAll().map { it.price }).containsExactlyInAnyOrder(1_490_000, 1_390_000)
        assertThat(recommendationScoreRepository.findAll()).hasSize(RecommendationUseCase.entries.size)

        val recommendations = recommendLaptopsUseCase.recommend(
            LaptopRecommendationQuery.fixture(
                budget = 2_000_000,
                maxWeightKg = 2.0,
                useCase = RecommendationUseCase.OFFICE_STUDY,
            ),
            PageQuery(page = 0, size = 10),
        )

        assertThat(recommendations.content.map { it.name }).containsExactly("Postgres Verified 14")
    }

    private fun crawledLaptop(
        name: String = "Postgres Verified",
        price: Int,
    ): CrawledLaptopCommand {
        return CrawledLaptopCommand(
            name = name,
            imageUrl = "https://example.com/postgres-verified.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=PG001&cate=112758",
            productCode = "PG001",
            price = price,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra 5 225U",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "1920x1200(WUXGA)",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Graphics",
            tgp = 0,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.35,
            lastDetailedCrawledAt = null,
            usages = listOf("사무/인강용"),
        )
    }

    companion object {
        private val postgresContainer: PostgreSQLContainer<*>? =
            if (
                System.getenv("POSTGRES_INTEGRATION_TESTS") == "true" &&
                System.getenv("POSTGRES_INTEGRATION_JDBC_URL").isNullOrBlank()
            ) {
                PostgreSQLContainer(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("laptopgg_test")
                    .withUsername("laptopgg")
                    .withPassword("laptopgg")
                    .also { it.start() }
            } else {
                null
            }

        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            val externalJdbcUrl = System.getenv("POSTGRES_INTEGRATION_JDBC_URL")
            val container = postgresContainer
            registry.add("spring.datasource.url") {
                externalJdbcUrl ?: requireNotNull(container).jdbcUrl
            }
            registry.add("spring.datasource.username") {
                System.getenv("POSTGRES_INTEGRATION_USERNAME") ?: container?.username ?: "laptopgg"
            }
            registry.add("spring.datasource.password") {
                System.getenv("POSTGRES_INTEGRATION_PASSWORD") ?: container?.password ?: "laptopgg"
            }
        }

        @JvmStatic
        @AfterAll
        fun stopPostgresContainer() {
            postgresContainer?.stop()
        }
    }
}
