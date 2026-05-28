package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.application.crawler.persistence.port.ExistingCrawledLaptopLookupPort
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
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        recommendationScoreRefresher = recommendationScoreService,
    )
    private val service = SaveCrawledLaptopService(
        laptopPort = laptopPort,
        existingLookupLoader = ExistingCrawledLaptopLookupLoader(laptopPort),
        postSaveSynchronizer = CrawledLaptopPostSaveSynchronizer(
            laptopProfileSynchronizer = laptopProfileService,
            laptopPriceHistoryRecorder = LaptopPriceHistoryService(
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

    @Test
    fun `saveOrUpdate normalizes usage values before persistence`() {
        service.saveOrUpdateLaptop(
            crawledLaptop(usages = listOf(" 사무/인강용 ", "", "사무/인강용", "영상편집")),
        )

        assertThat(laptopPort.createdCommand?.usages)
            .containsExactly("사무/인강용", "영상편집")
    }

    @Test
    fun `saveOrUpdate normalizes crawler identity and text values before lookup and persistence`() {
        service.saveOrUpdateLaptop(
            crawledLaptop(
                name = " Transaction Boundary 14 ",
                imageUrl = " https://example.com/transaction-boundary.jpg ",
                detailPage = " https://prod.danawa.com/info/?pcode=TX001&cate=112758 ",
                productCode = " TX001 ",
                cpu = " Core Ultra 5 225U ",
                ramType = " ",
            ),
        )

        assertThat(laptopPort.createdCommand?.name).isEqualTo("Transaction Boundary 14")
        assertThat(laptopPort.createdCommand?.imageUrl).isEqualTo("https://example.com/transaction-boundary.jpg")
        assertThat(laptopPort.createdCommand?.detailPage).isEqualTo("https://prod.danawa.com/info/?pcode=TX001&cate=112758")
        assertThat(laptopPort.createdCommand?.productCode).isEqualTo("TX001")
        assertThat(laptopPort.createdCommand?.cpu).isEqualTo("Core Ultra 5 225U")
        assertThat(laptopPort.createdCommand?.ramType).isNull()
    }

    @Test
    fun `saveOrUpdate uses normalized product code when finding existing laptop`() {
        laptopPort.existingByProductCode["TX001"] = crawledLaptop(productCode = "TX001").toPersistedSnapshot(id = 7L)

        val result = service.saveOrUpdateLaptop(crawledLaptop(productCode = " TX001 "))

        assertThat(result).isEqualTo(SaveResult.UNCHANGED)
        assertThat(laptopPort.createdCommand).isNull()
        assertThat(profilePort.saved.first().laptopId).isEqualTo(7L)
    }

    @Test
    fun `unchanged detail snapshot still refreshes profile and recommendation scores`() {
        val command = crawledLaptop()
        laptopPort.existingByProductCode[command.productCode!!] = command.toPersistedSnapshot(id = 7L)

        val result = service.saveOrUpdateLaptop(command)

        assertThat(result).isEqualTo(SaveResult.UNCHANGED)
        assertThat(profilePort.saved).hasSize(1)
        assertThat(profilePort.saved.first().laptopId).isEqualTo(7L)
        assertThat(recommendationScorePort.saved).hasSize(RecommendationUseCase.entries.size)
        assertThat(priceHistoryPort.saved).isEmpty()
    }

    @Test
    fun `changed detail snapshot merges saved state without a second full graph load`() {
        val existing = crawledLaptop(price = 1_490_000, usages = listOf("사무/인강용"))
            .toPersistedSnapshot(id = 7L)
        laptopPort.existingByProductCode["TX001"] = existing

        val result = service.saveOrUpdateLaptop(
            crawledLaptop(price = 1_390_000, usages = listOf("영상편집")),
        )

        assertThat(result).isEqualTo(SaveResult.UPDATED)
        assertThat(laptopPort.findWithUsageCalls).isZero()
        assertThat(laptopPort.detailUpdates).hasSize(1)
        val update = laptopPort.detailUpdates.single()
        assertThat(update.laptopId).isEqualTo(7L)
        assertThat(update.command.price).isEqualTo(1_390_000)
        assertThat(update.command.usages).containsExactly("영상편집")
        assertThat(profilePort.saved.single().laptopId).isEqualTo(7L)
        assertThat(priceHistoryPort.saved.map { it.price }).containsExactly(1_390_000)
        assertThat(recommendationScorePort.saved).hasSize(RecommendationUseCase.entries.size)
    }

    @Test
    fun `saveListSnapshot rejects missing existing laptop with explicit crawler error`() {
        assertThatThrownBy {
            service.saveListSnapshot(existingLaptopId = 404L, productCard = crawledProductCard())
        }.isInstanceOf(CrawlerResourceNotFoundException::class.java)
    }

    @Test
    fun `saveListSnapshot uses list snapshot path without loading full laptop usages`() {
        laptopPort.listSnapshots[7L] = PersistedCrawledListSnapshot(
            id = 7L,
            name = "Old Name",
            imageUrl = "https://example.com/old.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=LIST001&cate=112758",
            productCode = "LIST001",
            price = 1_290_000,
        )

        val result = service.saveListSnapshot(
            existingLaptopId = 7L,
            productCard = crawledProductCard(
                productCode = "LIST001",
                productName = "New Name",
                detailPage = "https://prod.danawa.com/info/?pcode=LIST001&cate=112758",
                imageUrl = "https://example.com/new.jpg",
                price = 1_190_000,
            ),
        )

        assertThat(result).isEqualTo(SaveResult.UPDATED)
        assertThat(laptopPort.findWithUsageCalls).isZero()
        assertThat(laptopPort.listUpdates).hasSize(1)
        val update = laptopPort.listUpdates.single()
        assertThat(update.laptopId).isEqualTo(7L)
        assertThat(update.command.name).isEqualTo("New Name")
        assertThat(update.command.imageUrl).isEqualTo("https://example.com/new.jpg")
        assertThat(update.command.price).isEqualTo(1_190_000)
        assertThat(profilePort.saved).isEmpty()
        assertThat(priceHistoryPort.saved.map { it.price }).containsExactly(1_190_000)
    }

    @Test
    fun `loadExistingLookup rejects invalid product card before persistence`() {
        assertThatThrownBy {
            service.loadExistingLookup(listOf(crawledProductCard(productCode = "")))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)

        assertThat(transactionPort.readCount).isZero()
    }

    @Test
    fun `saveListSnapshot rejects invalid command before persistence`() {
        assertThatThrownBy {
            service.saveListSnapshot(existingLaptopId = 0L, productCard = crawledProductCard())
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.saveListSnapshot(existingLaptopId = 1L, productCard = crawledProductCard(price = -1))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)

        assertThat(transactionPort.writeCount).isZero()
    }

    @Test
    fun `saveOrUpdate rejects invalid laptop command before persistence`() {
        assertThatThrownBy {
            service.saveOrUpdateLaptop(crawledLaptop(name = ""))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.saveOrUpdateLaptop(crawledLaptop(imageUrl = ""))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.saveOrUpdateLaptop(crawledLaptop(detailPage = ""))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
        assertThatThrownBy {
            service.saveOrUpdateLaptop(crawledLaptop(price = -1))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)

        assertThat(transactionPort.writeCount).isZero()
    }

    private fun crawledProductCard(
        productCode: String = "MISS001",
        productName: String = "Missing Existing Laptop 14",
        detailPage: String = "https://prod.danawa.com/info/?pcode=MISS001&cate=112758",
        imageUrl: String = "https://example.com/missing-existing.jpg",
        price: Int? = 1_290_000,
    ): CrawledProductCardCommand {
        return CrawledProductCardCommand(
            productName = productName,
            imageUrl = imageUrl,
            detailPage = detailPage,
            productCode = productCode,
            price = price,
        )
    }

    private fun crawledLaptop(
        name: String = "Transaction Boundary 14",
        imageUrl: String = "https://example.com/transaction-boundary.jpg",
        detailPage: String = "https://prod.danawa.com/info/?pcode=TX001&cate=112758",
        productCode: String? = "TX001",
        price: Int? = 1_490_000,
        cpu: String? = "Core Ultra 5 225U",
        ramType: String? = "LPDDR5X",
        usages: List<String> = listOf("사무/인강용"),
    ): CrawledLaptopCommand {
        return CrawledLaptopCommand(
            name = name,
            imageUrl = imageUrl,
            detailPage = detailPage,
            productCode = productCode,
            price = price,
            cpuManufacturer = "인텔",
            cpu = cpu,
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "1920x1200(WUXGA)",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = ramType,
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
            usages = usages,
        )
    }

    private class NonNestedTransactionPort : CrawlerTransactionPort {
        var readCount = 0
            private set
        var writeCount = 0
            private set
        private var active = false

        override fun <T> read(block: () -> T): T {
            readCount++
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

    private class InMemoryCrawledLaptopPersistencePort : CrawledLaptopPersistencePort, ExistingCrawledLaptopLookupPort {
        val existingByProductCode = mutableMapOf<String, PersistedCrawledLaptopSnapshot>()
        val listSnapshots = mutableMapOf<Long, PersistedCrawledListSnapshot>()
        val listUpdates = mutableListOf<ListSnapshotUpdate>()
        val detailUpdates = mutableListOf<DetailSnapshotUpdate>()
        var createdCommand: CrawledLaptopCommand? = null
            private set
        var findWithUsageCalls = 0
            private set

        override fun findListSnapshotById(laptopId: Long): PersistedCrawledListSnapshot? = listSnapshots[laptopId]

        override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? {
            findWithUsageCalls++
            return null
        }

        override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? = existingByProductCode[productCode]
        override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? = null
        override fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot> = emptyList()
        override fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot> = emptyList()

        override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
            createdCommand = command
            return command.toPersistedSnapshot(id = 1L)
        }

        override fun updateListSnapshot(laptopId: Long, command: UpdateCrawledListSnapshotCommand): Boolean {
            val current = listSnapshots[laptopId] ?: return false
            listUpdates += ListSnapshotUpdate(laptopId, command)
            listSnapshots[laptopId] = current.copy(
                name = command.name ?: current.name,
                imageUrl = command.imageUrl ?: current.imageUrl,
                detailPage = command.detailPage ?: current.detailPage,
                productCode = command.productCode ?: current.productCode,
                price = command.price ?: current.price,
            )
            return true
        }

        override fun updateDetailSnapshot(laptopId: Long, command: UpdateCrawledLaptopCommand): Boolean {
            val currentEntry = existingByProductCode.entries.firstOrNull { it.value.id == laptopId } ?: return false
            detailUpdates += DetailSnapshotUpdate(laptopId, command)
            existingByProductCode[currentEntry.key] = currentEntry.value.copy(
                name = command.name ?: currentEntry.value.name,
                imageUrl = command.imageUrl ?: currentEntry.value.imageUrl,
                detailPage = command.detailPage ?: currentEntry.value.detailPage,
                productCode = command.productCode ?: currentEntry.value.productCode,
                price = command.price ?: currentEntry.value.price,
                cpuManufacturer = command.cpuManufacturer ?: currentEntry.value.cpuManufacturer,
                cpu = command.cpu ?: currentEntry.value.cpu,
                os = command.os ?: currentEntry.value.os,
                screenSize = command.screenSize ?: currentEntry.value.screenSize,
                resolution = command.resolution ?: currentEntry.value.resolution,
                brightness = command.brightness ?: currentEntry.value.brightness,
                refreshRate = command.refreshRate ?: currentEntry.value.refreshRate,
                ramSize = command.ramSize ?: currentEntry.value.ramSize,
                ramType = command.ramType ?: currentEntry.value.ramType,
                isRamReplaceable = command.isRamReplaceable ?: currentEntry.value.isRamReplaceable,
                graphicsType = command.graphicsType ?: currentEntry.value.graphicsType,
                tgp = command.tgp ?: currentEntry.value.tgp,
                thunderboltCount = command.thunderboltCount ?: currentEntry.value.thunderboltCount,
                usbCCount = command.usbCCount ?: currentEntry.value.usbCCount,
                usbACount = command.usbACount ?: currentEntry.value.usbACount,
                sdCard = command.sdCard ?: currentEntry.value.sdCard,
                isSupportsPdCharging = command.isSupportsPdCharging ?: currentEntry.value.isSupportsPdCharging,
                batteryCapacity = command.batteryCapacity ?: currentEntry.value.batteryCapacity,
                storageCapacity = command.storageCapacity ?: currentEntry.value.storageCapacity,
                storageSlotCount = command.storageSlotCount ?: currentEntry.value.storageSlotCount,
                weight = command.weight ?: currentEntry.value.weight,
                lastDetailedCrawledAt = command.lastDetailedCrawledAt ?: currentEntry.value.lastDetailedCrawledAt,
                usages = command.usages ?: currentEntry.value.usages,
            )
            return true
        }

        data class ListSnapshotUpdate(
            val laptopId: Long,
            val command: UpdateCrawledListSnapshotCommand,
        )

        data class DetailSnapshotUpdate(
            val laptopId: Long,
            val command: UpdateCrawledLaptopCommand,
        )
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
