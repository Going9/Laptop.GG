package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopCpu
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopCpuRepository : JpaRepository<LaptopCpu, Long> {
}