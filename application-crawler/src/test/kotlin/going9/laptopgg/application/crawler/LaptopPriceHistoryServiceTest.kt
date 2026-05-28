package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.domain.laptop.Laptop
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LaptopPriceHistoryServiceTest {
    private val laptopPriceHistoryPort = InMemoryLaptopPriceHistoryPort()
    private val service = LaptopPriceHistoryService(laptopPriceHistoryPort)

    @Test
    fun `records current price through command boundary`() {
        service.recordCurrentPrice(laptop(id = 10L, price = 1_490_000), previousPrice = 1_590_000)

        assertThat(laptopPriceHistoryPort.saved).hasSize(1)
        assertThat(laptopPriceHistoryPort.saved.first().laptopId).isEqualTo(10L)
        assertThat(laptopPriceHistoryPort.saved.first().price).isEqualTo(1_490_000)
        assertThat(laptopPriceHistoryPort.saved.first().capturedAt).isNotNull()
    }

    @Test
    fun `skips price history when price is unchanged or missing`() {
        service.recordCurrentPrice(laptop(id = 11L, price = 1_490_000), previousPrice = 1_490_000)
        service.recordCurrentPrice(laptop(id = 12L, price = null), previousPrice = 1_490_000)

        assertThat(laptopPriceHistoryPort.saved).isEmpty()
    }

    private fun laptop(id: Long, price: Int?): Laptop {
        return Laptop(
            name = "Laptop $id",
            imageUrl = "https://example.com/$id.jpg",
            detailPage = "https://example.com/$id",
            productCode = id.toString(),
            price = price,
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
            id = id,
        )
    }

    private class InMemoryLaptopPriceHistoryPort : LaptopPriceHistoryPort {
        val saved = mutableListOf<RecordPriceHistoryCommand>()

        override fun save(command: RecordPriceHistoryCommand) {
            saved += command
        }
    }
}
