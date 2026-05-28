package going9.laptopgg.application.crawler.price

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import java.time.LocalDateTime

class LaptopPriceHistoryService(
    private val laptopPriceHistoryPort: LaptopPriceHistoryPort,
    private val transactionPort: CrawlerTransactionPort,
) {
    fun recordCurrentPrice(laptopId: Long, currentPrice: Int?, previousPrice: Int?) {
        transactionPort.write {
            recordCurrentPriceInTransaction(laptopId, currentPrice, previousPrice)
        }
    }

    private fun recordCurrentPriceInTransaction(laptopId: Long, currentPrice: Int?, previousPrice: Int?) {
        val price = currentPrice ?: return

        if (previousPrice == price) {
            return
        }

        laptopPriceHistoryPort.save(
            RecordPriceHistoryCommand(
                laptopId = laptopId,
                price = price,
                capturedAt = LocalDateTime.now(),
            ),
        )
    }
}
