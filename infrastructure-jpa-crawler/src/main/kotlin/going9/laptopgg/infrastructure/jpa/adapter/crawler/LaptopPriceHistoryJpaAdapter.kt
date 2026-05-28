package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.price.RecordPriceHistoryCommand
import going9.laptopgg.application.crawler.price.port.LaptopPriceHistoryPort
import going9.laptopgg.infrastructure.jpa.repository.crawler.LaptopPriceHistoryRepository
import going9.laptopgg.persistence.model.crawler.LaptopPriceHistory
import going9.laptopgg.persistence.model.laptop.Laptop
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
internal class LaptopPriceHistoryJpaAdapter(
    private val laptopPriceHistoryRepository: LaptopPriceHistoryRepository,
    private val entityManager: EntityManager,
) : LaptopPriceHistoryPort {
    override fun save(command: RecordPriceHistoryCommand) {
        laptopPriceHistoryRepository.save(
            LaptopPriceHistory(
                laptop = entityManager.getReference(Laptop::class.java, command.laptopId),
                price = command.price,
                capturedAt = command.capturedAt,
            ),
        )
    }
}
