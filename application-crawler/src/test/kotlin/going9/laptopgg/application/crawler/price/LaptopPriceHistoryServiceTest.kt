package going9.laptopgg.application.crawler.price

import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.application.crawler.support.InMemoryCrawlerTransactionPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LaptopPriceHistoryServiceTest {
    private val laptopPriceHistoryPort = InMemoryLaptopPriceHistoryPort()
    private val service = LaptopPriceHistoryService(
        laptopPriceHistoryPort = laptopPriceHistoryPort,
        transactionPort = InMemoryCrawlerTransactionPort(),
    )

    @Test
    fun `records current price through command boundary`() {
        service.recordCurrentPrice(laptopId = 10L, currentPrice = 1_490_000, previousPrice = 1_590_000)

        assertThat(laptopPriceHistoryPort.saved).hasSize(1)
        assertThat(laptopPriceHistoryPort.saved.first().laptopId).isEqualTo(10L)
        assertThat(laptopPriceHistoryPort.saved.first().price).isEqualTo(1_490_000)
        assertThat(laptopPriceHistoryPort.saved.first().capturedAt).isNotNull()
    }

    @Test
    fun `skips price history when price is unchanged or missing`() {
        service.recordCurrentPrice(laptopId = 11L, currentPrice = 1_490_000, previousPrice = 1_490_000)
        service.recordCurrentPrice(laptopId = 12L, currentPrice = null, previousPrice = 1_490_000)

        assertThat(laptopPriceHistoryPort.saved).isEmpty()
    }

    private class InMemoryLaptopPriceHistoryPort : LaptopPriceHistoryPort {
        val saved = mutableListOf<RecordPriceHistoryCommand>()

        override fun save(command: RecordPriceHistoryCommand) {
            saved += command
        }
    }
}
