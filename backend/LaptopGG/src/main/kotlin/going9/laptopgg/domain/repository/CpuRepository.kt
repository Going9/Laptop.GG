package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Cpu
import org.springframework.data.jpa.repository.JpaRepository

interface CpuRepository : JpaRepository<Cpu, Long> {
}