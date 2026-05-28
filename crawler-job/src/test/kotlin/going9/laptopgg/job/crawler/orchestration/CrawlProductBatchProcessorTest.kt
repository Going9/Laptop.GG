package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopLookup
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.SaveResult
import going9.laptopgg.job.crawler.detail.BuildLaptopResult
import going9.laptopgg.job.crawler.danawa.detail.DanawaDetailCrawler
import going9.laptopgg.job.crawler.detail.DetailRefreshOutcome
import going9.laptopgg.job.crawler.detail.DetailRefreshWorkItem
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.toCommand
import java.time.LocalDateTime
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CrawlProductBatchProcessorTest {
    private val saveCrawledLaptopUseCase = Mockito.mock(SaveCrawledLaptopUseCase::class.java)
    private val detailCrawler = Mockito.mock(DanawaDetailCrawler::class.java)
    private val snapshotSaver = CrawlProductSnapshotSaver(saveCrawledLaptopUseCase)
    private val detailRefreshOutcomeHandler = DetailRefreshOutcomeHandler(snapshotSaver)
    private val processor = CrawlProductBatchProcessor(
        saveCrawledLaptopUseCase,
        detailCrawler,
        snapshotSaver,
        detailRefreshOutcomeHandler,
    )

    @Test
    fun `fresh existing product is saved as list snapshot without detail refresh`() {
        val productCard = productCard("100")
        val existingLaptop = existingLaptop(id = 1L, productCode = productCard.productCode)
        Mockito.`when`(saveCrawledLaptopUseCase.loadExistingLookup(listOf(productCard.toCommand())))
            .thenReturn(
                ExistingCrawledLaptopLookup(
                    byProductCode = mapOf(productCard.productCode to existingLaptop),
                    byDetailPage = emptyMap(),
                ),
            )
        Mockito.`when`(saveCrawledLaptopUseCase.saveListSnapshot(existingLaptop.id, productCard.toCommand()))
            .thenReturn(SaveResult.UPDATED)
        val progress = CrawlProgress()
        val executor = Executors.newSingleThreadExecutor()

        try {
            val result = processor.process(listOf(productCard), progress, executor)

            assertThat(result.processedCount).isEqualTo(1)
            assertThat(result.detailRefreshCount).isEqualTo(0)
            assertThat(result.pagePriceOnlyUpdatedCount).isEqualTo(1)
            assertThat(progress.toSummary().updatedCount).isEqualTo(1)
            Mockito.verifyNoInteractions(detailCrawler)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `new product is fetched in detail and saved as full snapshot`() {
        val productCard = productCard("200")
        val executor = Executors.newSingleThreadExecutor()
        val workItems = listOf(DetailRefreshWorkItem(productCard = productCard, existingLaptop = null))
        val detailOutcome = DetailRefreshOutcome(
            workItem = workItems.first(),
            buildResult = BuildLaptopResult(command = crawledLaptopCommand(productCard), degradationReasons = emptyList()),
        )
        Mockito.`when`(saveCrawledLaptopUseCase.loadExistingLookup(listOf(productCard.toCommand())))
            .thenReturn(ExistingCrawledLaptopLookup(byProductCode = emptyMap(), byDetailPage = emptyMap()))
        Mockito.`when`(detailCrawler.fetchDetailRefreshOutcomes(workItems, executor))
            .thenReturn(listOf(detailOutcome))
        Mockito.`when`(saveCrawledLaptopUseCase.saveOrUpdateLaptop(detailOutcome.buildResult!!.command, null))
            .thenReturn(SaveResult.CREATED)
        val progress = CrawlProgress()

        try {
            val result = processor.process(listOf(productCard), progress, executor)

            assertThat(result.processedCount).isEqualTo(1)
            assertThat(result.detailRefreshCount).isEqualTo(1)
            assertThat(result.pagePriceOnlyUpdatedCount).isEqualTo(0)
            assertThat(progress.toSummary().createdCount).isEqualTo(1)
        } finally {
            executor.shutdown()
        }
    }

    private fun productCard(code: String): ProductCard {
        return ProductCard(
            productCode = code,
            productName = "Laptop $code",
            detailPage = "https://prod.danawa.com/info/?pcode=$code&cate=112758",
            imageUrl = "https://img.danawa.com/$code.jpg",
            price = 1_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }

    private fun existingLaptop(id: Long, productCode: String): ExistingCrawledLaptopSnapshot {
        return ExistingCrawledLaptopSnapshot(
            id = id,
            productCode = productCode,
            detailPage = "https://prod.danawa.com/info/?pcode=$productCode&cate=112758",
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11",
            screenSize = 14,
            resolution = "1920x1200",
            ramSize = 16,
            graphicsType = "Intel Graphics",
            storageCapacity = 512,
            batteryCapacity = 60.0,
            weight = 1.2,
            lastDetailedCrawledAt = LocalDateTime.now(),
            usageCount = 1,
        )
    }

    private fun crawledLaptopCommand(productCard: ProductCard): CrawledLaptopCommand {
        return CrawledLaptopCommand(
            name = productCard.productName,
            imageUrl = productCard.imageUrl,
            detailPage = productCard.detailPage,
            productCode = productCard.productCode,
            price = productCard.price,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11",
            screenSize = 14,
            resolution = "1920x1200",
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
            weight = 1.2,
            lastDetailedCrawledAt = LocalDateTime.now(),
            usages = listOf("사무/인강용"),
        )
    }
}
