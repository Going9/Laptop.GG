package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopPriceHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopPriceHistoryRepository : JpaRepository<LaptopPriceHistory, Long>
