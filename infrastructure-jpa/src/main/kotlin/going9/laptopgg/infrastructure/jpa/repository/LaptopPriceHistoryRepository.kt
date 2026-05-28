package going9.laptopgg.infrastructure.jpa.repository

import going9.laptopgg.domain.laptop.LaptopPriceHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopPriceHistoryRepository : JpaRepository<LaptopPriceHistory, Long>
