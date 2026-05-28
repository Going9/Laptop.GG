package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.Laptop
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CrawlerLaptopRepository : JpaRepository<Laptop, Long> {
    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findAllByProductCodeIn(productCodes: Collection<String>): List<Laptop>

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findAllByDetailPageIn(detailPages: Collection<String>): List<Laptop>

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findByDetailPage(detailPage: String): Laptop?

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findByProductCode(productCode: String): Laptop?

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
