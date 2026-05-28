package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.domain.laptop.LaptopProfile
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CrawlerLaptopProfileRepository : JpaRepository<LaptopProfile, Long> {
    fun findByLaptopId(laptopId: Long): LaptopProfile?

    @Query(
        """
        select p.laptop.id
        from LaptopProfile p
        where p.cpuPerformanceScore = 0
          and p.lowPowerCpuScore = 0
          and p.gpuPerformanceScore = 0
          and p.portabilityScore = 0
          and p.displayScore = 0
          and p.ramScore = 0
          and p.tgpScore = 0
        order by p.laptop.id asc
        """,
    )
    fun findLaptopIdsWithIncompleteStaticScores(pageable: Pageable): List<Long>
}
