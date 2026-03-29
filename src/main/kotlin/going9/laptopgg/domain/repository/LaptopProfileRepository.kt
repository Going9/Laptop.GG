package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopProfile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LaptopProfileRepository : JpaRepository<LaptopProfile, Long> {
    fun findByLaptopId(laptopId: Long): LaptopProfile?

    @Query(
        """
        select distinct p
        from LaptopProfile p
        join fetch p.laptop l
        where l.price is not null
          and l.price <= :maxPrice
          and (l.weight is null or l.weight <= :maxWeight)
          and (
            :screenFilterEnabled = false
            or (
              :includeUnknownScreen = true
              and (l.screenSize is null or l.screenSize in :screenSizes)
            )
            or (
              :includeUnknownScreen = false
              and l.screenSize in :screenSizes
            )
          )
          and (:minOfficeScore is null or p.officeScore >= :minOfficeScore)
          and (:minBatteryScore is null or p.batteryScore >= :minBatteryScore)
          and (:minCasualGameScore is null or p.casualGameScore >= :minCasualGameScore)
          and (:minOnlineGameScore is null or p.onlineGameScore >= :minOnlineGameScore)
          and (:minAaaGameScore is null or p.aaaGameScore >= :minAaaGameScore)
          and (:minCreatorScore is null or p.creatorScore >= :minCreatorScore)
          and (
            :minNotSureGateTotal is null
            or (p.officeScore + p.batteryScore + p.casualGameScore) >= :minNotSureGateTotal
          )
        """,
    )
    fun findRecommendationCandidates(
        @Param("maxPrice") maxPrice: Int,
        @Param("maxWeight") maxWeight: Double,
        @Param("screenSizes") screenSizes: Collection<Int>,
        @Param("screenFilterEnabled") screenFilterEnabled: Boolean,
        @Param("includeUnknownScreen") includeUnknownScreen: Boolean,
        @Param("minOfficeScore") minOfficeScore: Int?,
        @Param("minBatteryScore") minBatteryScore: Int?,
        @Param("minCasualGameScore") minCasualGameScore: Int?,
        @Param("minOnlineGameScore") minOnlineGameScore: Int?,
        @Param("minAaaGameScore") minAaaGameScore: Int?,
        @Param("minCreatorScore") minCreatorScore: Int?,
        @Param("minNotSureGateTotal") minNotSureGateTotal: Int?,
    ): List<LaptopProfile>

    @EntityGraph(attributePaths = ["laptop"])
    @Query(
        value = """
        select p
        from LaptopProfile p
        join p.laptop l
        where l.price is not null
          and l.price <= :maxPrice
          and (l.weight is null or l.weight <= :maxWeight)
          and (
            :screenFilterEnabled = false
            or (
              :includeUnknownScreen = true
              and (l.screenSize is null or l.screenSize in :screenSizes)
            )
            or (
              :includeUnknownScreen = false
              and l.screenSize in :screenSizes
            )
          )
          and (:minOfficeScore is null or p.officeScore >= :minOfficeScore)
          and (:minBatteryScore is null or p.batteryScore >= :minBatteryScore)
          and (:minCasualGameScore is null or p.casualGameScore >= :minCasualGameScore)
          and (:minOnlineGameScore is null or p.onlineGameScore >= :minOnlineGameScore)
          and (:minAaaGameScore is null or p.aaaGameScore >= :minAaaGameScore)
          and (:minCreatorScore is null or p.creatorScore >= :minCreatorScore)
          and (
            :minNotSureGateTotal is null
            or (p.officeScore + p.batteryScore + p.casualGameScore) >= :minNotSureGateTotal
          )
        order by
          case when :sortMode = 'recommended' then
            case
              when :useCase = 'NOT_SURE' then
                (p.officeScore * 0.24) +
                (p.batteryScore * 0.20) +
                (p.portabilityScore * 0.16) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.14) +
                (p.displayScore * 0.10) +
                (p.ramScore * 0.08) +
                (p.gpuPerformanceScore * 0.08)
              when :useCase = 'OFFICE_STUDY' then
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.25) +
                (p.portabilityScore * 0.20) +
                (p.batteryScore * 0.15) +
                (p.displayScore * 0.10) +
                (p.officeScore * 0.30)
              when :useCase = 'PORTABLE_OFFICE' then
                (p.portabilityScore * 0.35) +
                (p.batteryScore * 0.25) +
                (p.officeScore * 0.20) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10) +
                (p.displayScore * 0.10)
              when :useCase = 'BATTERY_FIRST' then
                (p.batteryScore * 0.45) +
                (p.portabilityScore * 0.20) +
                (p.officeScore * 0.15) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10) +
                (p.lowPowerCpuScore * 0.10)
              when :useCase = 'CASUAL_GAME' then
                (p.gpuPerformanceScore * 0.35) +
                (p.cpuPerformanceScore * 0.20) +
                (p.ramScore * 0.15) +
                (p.displayScore * 0.10) +
                (p.portabilityScore * 0.10) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10)
              when :useCase = 'ONLINE_GAME' then
                (p.gpuPerformanceScore * 0.40) +
                (p.cpuPerformanceScore * 0.20) +
                (p.ramScore * 0.15) +
                (p.tgpScore * 0.10) +
                (p.displayScore * 0.10) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.05)
              when :useCase = 'AAA_GAME' then
                (p.gpuPerformanceScore * 0.45) +
                (p.tgpScore * 0.20) +
                (p.cpuPerformanceScore * 0.15) +
                (p.ramScore * 0.10) +
                (p.displayScore * 0.05) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.05)
              else
                (p.cpuPerformanceScore * 0.20) +
                ((p.gpuPerformanceScore + p.gpuCreatorBonus) * 0.20) +
                (p.ramScore * 0.20) +
                (p.displayScore * 0.20) +
                (p.batteryScore * 0.05) +
                (p.portabilityScore * 0.05) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10)
            end
          end desc,
          case when :sortMode = 'price_asc' then l.price end asc,
          case when :sortMode = 'price_desc' then l.price end desc,
          case
            when :sortMode in ('weight_asc', 'weight_desc') and l.weight is null then 1
            when :sortMode in ('weight_asc', 'weight_desc') then 0
            else null
          end asc,
          case when :sortMode = 'weight_asc' then l.weight end asc,
          case when :sortMode = 'weight_desc' then l.weight end desc,
          case when :sortMode <> 'recommended' then
            case
              when :useCase = 'NOT_SURE' then
                (p.officeScore * 0.24) +
                (p.batteryScore * 0.20) +
                (p.portabilityScore * 0.16) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.14) +
                (p.displayScore * 0.10) +
                (p.ramScore * 0.08) +
                (p.gpuPerformanceScore * 0.08)
              when :useCase = 'OFFICE_STUDY' then
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.25) +
                (p.portabilityScore * 0.20) +
                (p.batteryScore * 0.15) +
                (p.displayScore * 0.10) +
                (p.officeScore * 0.30)
              when :useCase = 'PORTABLE_OFFICE' then
                (p.portabilityScore * 0.35) +
                (p.batteryScore * 0.25) +
                (p.officeScore * 0.20) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10) +
                (p.displayScore * 0.10)
              when :useCase = 'BATTERY_FIRST' then
                (p.batteryScore * 0.45) +
                (p.portabilityScore * 0.20) +
                (p.officeScore * 0.15) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10) +
                (p.lowPowerCpuScore * 0.10)
              when :useCase = 'CASUAL_GAME' then
                (p.gpuPerformanceScore * 0.35) +
                (p.cpuPerformanceScore * 0.20) +
                (p.ramScore * 0.15) +
                (p.displayScore * 0.10) +
                (p.portabilityScore * 0.10) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10)
              when :useCase = 'ONLINE_GAME' then
                (p.gpuPerformanceScore * 0.40) +
                (p.cpuPerformanceScore * 0.20) +
                (p.ramScore * 0.15) +
                (p.tgpScore * 0.10) +
                (p.displayScore * 0.10) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.05)
              when :useCase = 'AAA_GAME' then
                (p.gpuPerformanceScore * 0.45) +
                (p.tgpScore * 0.20) +
                (p.cpuPerformanceScore * 0.15) +
                (p.ramScore * 0.10) +
                (p.displayScore * 0.05) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.05)
              else
                (p.cpuPerformanceScore * 0.20) +
                ((p.gpuPerformanceScore + p.gpuCreatorBonus) * 0.20) +
                (p.ramScore * 0.20) +
                (p.displayScore * 0.20) +
                (p.batteryScore * 0.05) +
                (p.portabilityScore * 0.05) +
                ((case
                  when l.price is null or :budget <= 0 or l.price > :budget then 0.0
                  else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
                end) * 0.10)
            end
          end desc,
          l.price asc,
          l.id asc
        """,
        countQuery = """
        select count(p)
        from LaptopProfile p
        join p.laptop l
        where l.price is not null
          and l.price <= :maxPrice
          and (l.weight is null or l.weight <= :maxWeight)
          and (
            :screenFilterEnabled = false
            or (
              :includeUnknownScreen = true
              and (l.screenSize is null or l.screenSize in :screenSizes)
            )
            or (
              :includeUnknownScreen = false
              and l.screenSize in :screenSizes
            )
          )
          and (:minOfficeScore is null or p.officeScore >= :minOfficeScore)
          and (:minBatteryScore is null or p.batteryScore >= :minBatteryScore)
          and (:minCasualGameScore is null or p.casualGameScore >= :minCasualGameScore)
          and (:minOnlineGameScore is null or p.onlineGameScore >= :minOnlineGameScore)
          and (:minAaaGameScore is null or p.aaaGameScore >= :minAaaGameScore)
          and (:minCreatorScore is null or p.creatorScore >= :minCreatorScore)
          and (
            :minNotSureGateTotal is null
            or (p.officeScore + p.batteryScore + p.casualGameScore) >= :minNotSureGateTotal
          )
        """,
    )
    fun findRecommendationCandidatePage(
        @Param("maxPrice") maxPrice: Int,
        @Param("maxWeight") maxWeight: Double,
        @Param("screenSizes") screenSizes: Collection<Int>,
        @Param("screenFilterEnabled") screenFilterEnabled: Boolean,
        @Param("includeUnknownScreen") includeUnknownScreen: Boolean,
        @Param("minOfficeScore") minOfficeScore: Int?,
        @Param("minBatteryScore") minBatteryScore: Int?,
        @Param("minCasualGameScore") minCasualGameScore: Int?,
        @Param("minOnlineGameScore") minOnlineGameScore: Int?,
        @Param("minAaaGameScore") minAaaGameScore: Int?,
        @Param("minCreatorScore") minCreatorScore: Int?,
        @Param("minNotSureGateTotal") minNotSureGateTotal: Int?,
        @Param("budget") budget: Int,
        @Param("useCase") useCase: String,
        @Param("sortMode") sortMode: String,
        pageable: Pageable,
    ): Page<LaptopProfile>

    @Query(
        """
        select p
        from LaptopProfile p
        join fetch p.laptop l
        where p.cpuPerformanceScore = 0
          and p.lowPowerCpuScore = 0
          and p.gpuPerformanceScore = 0
          and p.portabilityScore = 0
          and p.displayScore = 0
          and p.ramScore = 0
          and p.tgpScore = 0
        """,
    )
    fun findAllIncompleteStaticScores(): List<LaptopProfile>

    @Query(
        """
        select count(p)
        from LaptopProfile p
        where p.cpuPerformanceScore = 0
          and p.lowPowerCpuScore = 0
          and p.gpuPerformanceScore = 0
          and p.portabilityScore = 0
          and p.displayScore = 0
          and p.ramScore = 0
          and p.tgpScore = 0
        """,
    )
    fun countIncompleteStaticScores(): Long

    @Query(
        """
        select distinct p
        from LaptopProfile p
        join fetch p.laptop l
        left join fetch l.laptopUsage
        """,
    )
    fun findAllWithLaptopAndUsage(): List<LaptopProfile>
}
