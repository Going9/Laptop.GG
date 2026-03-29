package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopProfile
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
