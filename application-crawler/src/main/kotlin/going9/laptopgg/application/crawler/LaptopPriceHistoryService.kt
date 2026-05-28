package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@Transactional
class LaptopPriceHistoryService(
    private val laptopPriceHistoryPort: LaptopPriceHistoryPort,
) {
    fun recordCurrentPrice(laptopId: Long, currentPrice: Int?, previousPrice: Int?) {
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
