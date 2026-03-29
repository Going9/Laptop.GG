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
        """,
    )
    fun findRecommendationCandidates(
        @Param("maxPrice") maxPrice: Int,
        @Param("maxWeight") maxWeight: Double,
    ): List<LaptopProfile>

    @Query(
        """
        select distinct p
        from LaptopProfile p
        join fetch p.laptop l
        where l.price is not null
          and l.price <= :maxPrice
          and (l.weight is null or l.weight <= :maxWeight)
          and l.screenSize in :screenSizes
        """,
    )
    fun findRecommendationCandidatesByScreenSizes(
        @Param("maxPrice") maxPrice: Int,
        @Param("maxWeight") maxWeight: Double,
        @Param("screenSizes") screenSizes: Collection<Int>,
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
