package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopGpu
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopGpuRepository : JpaRepository<LaptopGpu, Long> {
}