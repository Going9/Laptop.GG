package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class LaptopPriceHistoryService(
    private val laptopPriceHistoryPort: LaptopPriceHistoryPort,
) {
    fun recordCurrentPrice(laptop: Laptop, previousPrice: Int?) {
        val laptopId = requireNotNull(laptop.id) { "Laptop must be persisted before recording price history." }
        val currentPrice = laptop.price ?: return

        if (previousPrice == currentPrice) {
            return
        }

        laptopPriceHistoryPort.save(
            RecordPriceHistoryCommand(
                laptopId = laptopId,
                price = currentPrice,
                capturedAt = LocalDateTime.now(),
            ),
        )
    }
}
