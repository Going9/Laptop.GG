package going9.laptopgg.infrastructure.jpa.repository.shared

import going9.laptopgg.domain.laptop.LaptopUsage
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopUsageRepository : JpaRepository<LaptopUsage, Long> {
}
