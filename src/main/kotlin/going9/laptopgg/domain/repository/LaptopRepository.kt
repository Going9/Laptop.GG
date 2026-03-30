package going9.laptopgg.domain.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LaptopRepository : JpaRepository<Laptop, Long>, KotlinJdslJpqlExecutor {
    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findAllByProductCodeIn(productCodes: Collection<String>): List<Laptop>

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findAllByDetailPageIn(detailPages: Collection<String>): List<Laptop>

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findByName(name: String): Laptop?

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findByDetailPage(detailPage: String): Laptop?

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findAllByDetailPageContaining(detailPageToken: String): List<Laptop>

    @EntityGraph(attributePaths = ["laptopUsage"])
    fun findByProductCode(productCode: String): Laptop?

    @EntityGraph(attributePaths = ["laptopUsage"])
    @Query(
        """
        select distinct l
        from Laptop l
        left join fetch l.laptopUsage
        where l.id in :ids
        """,
    )
    fun findAllWithUsageByIdIn(@Param("ids") ids: Collection<Long>): List<Laptop>

    @Query(
        """
        select l.id
        from Laptop l
        where not exists (
            select 1
            from LaptopProfile p
            where p.laptop = l
        )
        order by l.id asc
        """,
    )
    fun findIdsWithoutProfile(pageable: Pageable): List<Long>

    @Query(
        """
        select distinct l
        from Laptop l
        left join fetch l.laptopUsage
        where not exists (
            select 1
            from LaptopProfile p
            where p.laptop = l
        )
        """,
    )
    fun findAllWithoutProfile(): List<Laptop>

    @Query(
        """
        select count(l)
        from Laptop l
        where not exists (
            select 1
            from LaptopProfile p
            where p.laptop = l
        )
        """,
    )
    fun countWithoutProfile(): Long
}
