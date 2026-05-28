package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.price.LaptopPriceHistoryService
import going9.laptopgg.application.crawler.price.RecordPriceHistoryCommand
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.profile.CpuClassifier
import going9.laptopgg.application.crawler.profile.CpuTokenResolver
import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.GpuClassifier
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.profile.ProfileScorePolicy
import going9.laptopgg.application.crawler.profile.UpsertCrawledLaptopProfileCommand
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SaveCrawledLaptopServiceTest {
    private val transactionPort = NonNestedTransactionPort()
    private val laptopPort = InMemoryCrawledLaptopPersistencePort()
    private val profilePort = InMemoryCrawledLaptopProfilePort()
    private val priceHistoryPort = InMemoryLaptopPriceHistoryPort()
    private val recommendationScorePort = InMemoryRecommendationScorePort()
    private val recommendationScoreService = RecommendationScoreService(
        recommendationScorePort = recommendationScorePort,
        transactionPort = transactionPort,
    )
    private val laptopProfileService = LaptopProfileService(
        laptopProfilePort = profilePort,
        laptopProfileFactory = LaptopProfileFactory(
            cpuClassifier = CpuClassifier(CpuTokenResolver()),
            gpuClassifier = GpuClassifier(),
            profileScorePolicy = ProfileScorePolicy(),
        ),
        recommendationScoreService = recommendationScoreService,
        transactionPort = transactionPort,
    )
    private val service = SaveCrawledLaptopService(
        laptopPort = laptopPort,
        existingLookupLoader = ExistingCrawledLaptopLookupLoader(laptopPort),
        postSaveSynchronizer = CrawledLaptopPostSaveSynchronizer(
            laptopProfileService = laptopProfileService,
            laptopPriceHistoryService = LaptopPriceHistoryService(
                laptopPriceHistoryPort = priceHistoryPort,
                transactionPort = transactionPort,
            ),
        ),
        transactionPort = transactionPort,
    )

    @Test
    fun `saveOrUpdate runs profile score and price side effects inside one write transaction`() {
        val result = service.saveOrUpdateLaptop(crawledLaptop())

        assertThat(result).isEqualTo(SaveResult.CREATED)
        assertThat(transactionPort.writeCount).isEqualTo(1)
        assertThat(profilePort.saved).hasSize(1)
        assertThat(recommendationScorePort.saved).hasSize(RecommendationUseCase.entries.size)
        assertThat(priceHistoryPort.saved.map { it.price }).containsExactly(1_490_000)
    }

    private fun crawledLaptop(): CrawledLaptopCommand {
        return CrawledLaptopCommand(
            name = "Transaction Boundary 14",
            imageUrl = "https://example.com/transaction-boundary.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=TX001&cate=112758",
            productCode = "TX001",
            price = 1_490_000,
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

    private class NonNestedTransactionPort : CrawlerTransactionPort {
        var writeCount = 0
            private set
        private var active = false

        override fun <T> read(block: () -> T): T {
            return enter(block)
        }

        override fun <T> write(block: () -> T): T {
            writeCount++
            return enter(block)
        }

        private fun <T> enter(block: () -> T): T {
            check(!active) { "Nested transaction entrypoint was called." }
            active = true
            return try {
                block()
            } finally {
                active = false
            }
        }
    }

    private class InMemoryCrawledLaptopPersistencePort : CrawledLaptopPersistencePort {
        override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? = null
        override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? = null
        override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? = null
        override fun findAllByProductCodes(productCodes: Collection<String>): List<PersistedCrawledLaptopSnapshot> = emptyList()
        override fun findAllByDetailPages(detailPages: Collection<String>): List<PersistedCrawledLaptopSnapshot> = emptyList()

        override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            return command.toPersistedSnapshot(id = 1L)
        }

        override fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            error("update is not used by this test")
        }
    }

    private class InMemoryCrawledLaptopProfilePort : CrawledLaptopProfilePort {
        val saved = mutableListOf<UpsertCrawledLaptopProfileCommand>()

        override fun upsert(command: UpsertCrawledLaptopProfileCommand): CrawledLaptopProfileState {
            saved += command
            return CrawledLaptopProfileState(
                laptopId = command.laptopId,
                profile = command.profile,
            )
        }

    }

    private class InMemoryLaptopPriceHistoryPort : LaptopPriceHistoryPort {
        val saved = mutableListOf<RecordPriceHistoryCommand>()

        override fun save(command: RecordPriceHistoryCommand) {
            saved += command
        }
    }

    private class InMemoryRecommendationScorePort : RecommendationScorePort {
        val saved = mutableListOf<UpsertRecommendationScoreCommand>()

        override fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>) {
            saved += scores
        }
    }
}

private fun CrawledLaptopCommand.toPersistedSnapshot(id: Long): PersistedCrawledLaptopSnapshot {
    return PersistedCrawledLaptopSnapshot(
        id = id,
        name = name,
        imageUrl = imageUrl,
        detailPage = detailPage,
        productCode = productCode,
        price = price,
        cpuManufacturer = cpuManufacturer,
        cpu = cpu,
        os = os,
        screenSize = screenSize,
        resolution = resolution,
        brightness = brightness,
        refreshRate = refreshRate,
        ramSize = ramSize,
        ramType = ramType,
        isRamReplaceable = isRamReplaceable,
        graphicsType = graphicsType,
        tgp = tgp,
        thunderboltCount = thunderboltCount,
        usbCCount = usbCCount,
        usbACount = usbACount,
        sdCard = sdCard,
        isSupportsPdCharging = isSupportsPdCharging,
        batteryCapacity = batteryCapacity,
        storageCapacity = storageCapacity,
        storageSlotCount = storageSlotCount,
        weight = weight,
        lastDetailedCrawledAt = lastDetailedCrawledAt,
        usages = usages,
    )
}
