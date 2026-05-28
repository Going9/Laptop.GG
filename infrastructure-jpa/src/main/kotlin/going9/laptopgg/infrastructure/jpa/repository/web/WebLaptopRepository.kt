package going9.laptopgg.infrastructure.jpa.repository.web

import going9.laptopgg.persistence.model.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WebLaptopRepository : JpaRepository<Laptop, Long> {
    @Query(
        """
        select distinct l
        from Laptop l
        left join fetch l.laptopUsage
        where l.id = :id
        """,
    )
    fun findWithUsageById(@Param("id") id: Long): Laptop?
}
