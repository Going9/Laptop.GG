package going9.laptopgg.integration

import going9.laptopgg.InfrastructureJpaTestApplication
import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.SaveResult
import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.LaptopPriceHistoryRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
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
class PostgresCrawlerPersistenceUseCaseTest {
    @Autowired
    lateinit var flyway: Flyway

    @Autowired
    lateinit var saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase

    @Autowired
    lateinit var recommendLaptopsUseCase: RecommendLaptopsUseCase

    @Autowired
    lateinit var crawlerRunLockUseCase: CrawlerRunLockUseCase

    @Autowired
    lateinit var laptopRepository: CrawlerLaptopRepository

    @Autowired
    lateinit var laptopProfileRepository: CrawlerLaptopProfileRepository

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
            LaptopRecommendationQuery(
                budget = 2_000_000,
                maxWeightKg = 2.0,
                useCase = RecommendationUseCase.OFFICE_STUDY,
            ),
            PageQuery(page = 0, size = 10),
        )

        assertThat(recommendations.content.map { it.name }).containsExactly("Postgres Verified 14")
    }

    @Test
    fun `crawler advisory lock rejects overlapping postgres runs and releases after block`() {
        val nestedAttempt = crawlerRunLockUseCase.runLocked {
            crawlerRunLockUseCase.runLocked {
                "nested"
            }
        }

        assertThat(nestedAttempt.acquired).isTrue()
        assertThat(nestedAttempt.value!!.acquired).isFalse()

        val afterRelease = crawlerRunLockUseCase.runLocked {
            "released"
        }

        assertThat(afterRelease.acquired).isTrue()
        assertThat(afterRelease.value).isEqualTo("released")
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
        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            PostgresIntegrationDatabase.registerSpringDatasource(registry)
        }
    }
}
