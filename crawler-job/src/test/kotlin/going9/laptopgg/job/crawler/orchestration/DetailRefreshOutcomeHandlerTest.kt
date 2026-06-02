package going9.laptopgg.job.crawler.orchestration

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.SaveResult
import going9.laptopgg.job.crawler.detail.BuildLaptopResult
import going9.laptopgg.job.crawler.detail.DetailRefreshOutcome
import going9.laptopgg.job.crawler.detail.DetailRefreshWorkItem
import going9.laptopgg.job.crawler.list.ProductCard
import going9.laptopgg.job.crawler.list.toCommand
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class DetailRefreshOutcomeHandlerTest {
    private val saveCrawledLaptopUseCase = Mockito.mock(SaveCrawledLaptopUseCase::class.java)
    private val snapshotSaver = CrawlProductSnapshotSaver(saveCrawledLaptopUseCase)
    private val handler = DetailRefreshOutcomeHandler(snapshotSaver)

    @Test
    fun `records degraded full snapshot saves`() {
        val productCard = productCard("100")
        val buildResult = BuildLaptopResult(
            command = crawledLaptopCommand(productCard),
            degradationReasons = listOf("summary fallback"),
        )
        Mockito.`when`(saveCrawledLaptopUseCase.saveOrUpdateLaptop(buildResult.command, null))
            .thenReturn(SaveResult.CREATED)
        val progress = CrawlProgress()

        val result = handler.handle(
            detailRefreshOutcomes = listOf(
                DetailRefreshOutcome(
                    workItem = DetailRefreshWorkItem(productCard = productCard, existingLaptop = null),
                    buildResult = buildResult,
                ),
            ),
            progress = progress,
        )

        assertThat(result.pagePriceOnlyUpdatedCount).isZero()
        assertThat(progress.toSummary().createdCount).isEqualTo(1)
        assertThat(progress.toSummary().degradedCount).isEqualTo(1)
        assertThat(progress.toSummary().degradedSamples).containsExactly("100 | Laptop 100 | summary fallback")
    }

    @Test
    fun `falls back to list snapshot and records failure when detail build failed for existing laptop`() {
        val productCard = productCard("200")
        val existingLaptop = existingLaptop(id = 2L, productCode = productCard.productCode)
        Mockito.`when`(saveCrawledLaptopUseCase.saveListSnapshot(existingLaptop.id, productCard.toCommand()))
            .thenReturn(SaveResult.UPDATED)
        val progress = CrawlProgress()

        val result = handler.handle(
            detailRefreshOutcomes = listOf(
                DetailRefreshOutcome(
                    workItem = DetailRefreshWorkItem(productCard = productCard, existingLaptop = existingLaptop),
                    error = IllegalStateException("detail failed"),
                ),
            ),
            progress = progress,
        )

        assertThat(result.pagePriceOnlyUpdatedCount).isEqualTo(1)
        assertThat(progress.toSummary().updatedCount).isEqualTo(1)
        assertThat(progress.toSummary().failedCount).isEqualTo(1)
        assertThat(progress.toSummary().failureSamples).containsExactly("200 | Laptop 200 | detail failed")
    }

    @Test
    fun `interrupted save failure is propagated instead of recorded as product failure`() {
        val productCard = productCard("300")
        val buildResult = BuildLaptopResult(
            command = crawledLaptopCommand(productCard),
            degradationReasons = emptyList(),
        )
        val failure = IllegalStateException("interrupted save", InterruptedException("stop"))
        Mockito.`when`(saveCrawledLaptopUseCase.saveOrUpdateLaptop(buildResult.command, null))
            .thenThrow(failure)
        val progress = CrawlProgress()

        try {
            assertThatThrownBy {
                handler.handle(
                    detailRefreshOutcomes = listOf(
                        DetailRefreshOutcome(
                            workItem = DetailRefreshWorkItem(productCard = productCard, existingLaptop = null),
                            buildResult = buildResult,
                        ),
                    ),
                    progress = progress,
                )
            }.isSameAs(failure)
            assertThat(Thread.currentThread().isInterrupted).isTrue()
            assertThat(progress.toSummary().failedCount).isZero()
        } finally {
            Thread.interrupted()
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
