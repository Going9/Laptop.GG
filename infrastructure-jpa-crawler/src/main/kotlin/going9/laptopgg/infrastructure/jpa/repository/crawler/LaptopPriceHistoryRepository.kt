package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.LaptopPriceHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopPriceHistoryRepository : JpaRepository<LaptopPriceHistory, Long>
