package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Ram
import org.springframework.data.jpa.repository.JpaRepository

interface RamRepository : JpaRepository<Ram, Long> {
}