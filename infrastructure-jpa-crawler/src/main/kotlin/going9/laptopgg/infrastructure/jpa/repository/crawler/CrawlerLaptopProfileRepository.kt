package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.LaptopProfile
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlerLaptopProfileRepository : JpaRepository<LaptopProfile, Long> {
    fun findByLaptopId(laptopId: Long): LaptopProfile?
}
