package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.crawler.LaptopPriceHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopPriceHistoryRepository : JpaRepository<LaptopPriceHistory, Long>
