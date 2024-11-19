package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.laptop.NewLaptop
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopUsageRepository : JpaRepository<LaptopUsage, Long> {
}