package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.LaptopUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CrawlerLaptopUsageRepository : JpaRepository<LaptopUsage, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LaptopUsage lu where lu.laptop.id = :laptopId")
    fun deleteByLaptopId(@Param("laptopId") laptopId: Long): Int
}
