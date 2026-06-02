package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.LaptopProfile
import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.CpuClass
import going9.laptopgg.taxonomy.GpuClass
import going9.laptopgg.taxonomy.PortabilityTier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CrawlerLaptopProfileRepository : JpaRepository<LaptopProfile, Long> {
    fun findByLaptopId(laptopId: Long): LaptopProfile?

    @Modifying(flushAutomatically = true)
    @Query(
        """
        update LaptopProfile p
        set p.cpuClass = :cpuClass,
            p.gpuClass = :gpuClass,
            p.batteryTier = :batteryTier,
            p.portabilityTier = :portabilityTier,
            p.officeScore = :officeScore,
            p.batteryScore = :batteryScore,
            p.casualGameScore = :casualGameScore,
            p.onlineGameScore = :onlineGameScore,
            p.aaaGameScore = :aaaGameScore,
            p.creatorScore = :creatorScore,
            p.cpuPerformanceScore = :cpuPerformanceScore,
            p.lowPowerCpuScore = :lowPowerCpuScore,
            p.gpuPerformanceScore = :gpuPerformanceScore,
            p.gpuCreatorBonus = :gpuCreatorBonus,
            p.portabilityScore = :portabilityScore,
            p.displayScore = :displayScore,
            p.ramScore = :ramScore,
            p.tgpScore = :tgpScore
        where p.laptop.id = :laptopId
        """,
    )
    fun updateByLaptopId(
        @Param("laptopId") laptopId: Long,
        @Param("cpuClass") cpuClass: CpuClass,
        @Param("gpuClass") gpuClass: GpuClass,
        @Param("batteryTier") batteryTier: BatteryTier,
        @Param("portabilityTier") portabilityTier: PortabilityTier,
        @Param("officeScore") officeScore: Int,
        @Param("batteryScore") batteryScore: Int,
        @Param("casualGameScore") casualGameScore: Int,
        @Param("onlineGameScore") onlineGameScore: Int,
        @Param("aaaGameScore") aaaGameScore: Int,
        @Param("creatorScore") creatorScore: Int,
        @Param("cpuPerformanceScore") cpuPerformanceScore: Int,
        @Param("lowPowerCpuScore") lowPowerCpuScore: Int,
        @Param("gpuPerformanceScore") gpuPerformanceScore: Int,
        @Param("gpuCreatorBonus") gpuCreatorBonus: Int,
        @Param("portabilityScore") portabilityScore: Int,
        @Param("displayScore") displayScore: Int,
        @Param("ramScore") ramScore: Int,
        @Param("tgpScore") tgpScore: Int,
    ): Int
}
