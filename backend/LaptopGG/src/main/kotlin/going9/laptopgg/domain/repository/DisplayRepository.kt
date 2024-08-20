package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Display
import org.springframework.data.jpa.repository.JpaRepository

interface DisplayRepository : JpaRepository<Display, Long> {
}