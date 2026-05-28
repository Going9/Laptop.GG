package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.RecordPriceHistoryCommand
import going9.laptopgg.application.crawler.port.out.LaptopPriceHistoryPort
import going9.laptopgg.persistence.model.laptop.LaptopPriceHistory
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.LaptopPriceHistoryRepository
import org.springframework.stereotype.Component

@Component
class LaptopPriceHistoryJpaAdapter(
    private val crawlerLaptopRepository: CrawlerLaptopRepository,
    private val laptopPriceHistoryRepository: LaptopPriceHistoryRepository,
) : LaptopPriceHistoryPort {
    override fun save(command: RecordPriceHistoryCommand) {
        laptopPriceHistoryRepository.save(
            LaptopPriceHistory(
                laptop = crawlerLaptopRepository.getReferenceById(command.laptopId),
                price = command.price,
                capturedAt = command.capturedAt,
            ),
        )
    }
}
