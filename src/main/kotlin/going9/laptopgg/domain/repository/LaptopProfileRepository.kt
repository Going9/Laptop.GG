package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.LaptopProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LaptopProfileRepository : JpaRepository<LaptopProfile, Long> {
    fun findByLaptopId(laptopId: Long): LaptopProfile?

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
