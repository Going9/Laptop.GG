package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Gpu
import org.springframework.data.jpa.repository.JpaRepository

interface GpuRepository : JpaRepository<Gpu, Long>{
}