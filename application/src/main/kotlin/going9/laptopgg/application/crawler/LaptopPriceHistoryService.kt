package going9.laptopgg.application.crawler

import going9.laptopgg.application.port.out.LaptopPriceHistoryPort
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopPriceHistory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LaptopPriceHistoryService(
    private val laptopPriceHistoryPort: LaptopPriceHistoryPort,
) {
    @Transactional
    fun recordCurrentPrice(laptop: Laptop, previousPrice: Int?) {
        requireNotNull(laptop.id) { "Laptop must be persisted before recording price history." }
        val currentPrice = laptop.price ?: return

        if (previousPrice == currentPrice) {
            return
        }

        laptopPriceHistoryPort.save(
            LaptopPriceHistory(
                laptop = laptop,
                price = currentPrice,
                capturedAt = LocalDateTime.now(),
            ),
        )
    }
}
