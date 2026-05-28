package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.domain.laptop.LaptopPriceHistory
import going9.laptopgg.infrastructure.jpa.repository.crawler.LaptopPriceHistoryRepository
import org.springframework.stereotype.Component

@Component
class LaptopPriceHistoryJpaAdapter(
    private val laptopPriceHistoryRepository: LaptopPriceHistoryRepository,
) : LaptopPriceHistoryPort {
    override fun save(priceHistory: LaptopPriceHistory): LaptopPriceHistory {
        return laptopPriceHistoryRepository.save(priceHistory)
    }
}
