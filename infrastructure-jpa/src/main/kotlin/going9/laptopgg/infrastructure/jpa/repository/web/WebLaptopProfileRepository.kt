package going9.laptopgg.infrastructure.jpa.repository.web

import going9.laptopgg.persistence.model.laptop.LaptopProfile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WebLaptopProfileRepository : JpaRepository<LaptopProfile, Long> {
    @EntityGraph(attributePaths = ["laptop"])
    @Query(
        value = """
        select p
        from LaptopProfile p
        join p.laptop l
        join RecommendationScore rs on rs.laptop = l
        where rs.useCase = :useCase
          and rs.gateScore >= :gateThreshold
          and l.price is not null
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
        order by
          case when :sortMode = 'recommended' then
            rs.staticScore +
            ((case
              when l.price is null or :budget <= 0 or l.price > :budget then 0.0
              else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
            end) * rs.budgetWeight)
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
            rs.staticScore +
            ((case
              when l.price is null or :budget <= 0 or l.price > :budget then 0.0
              else 60.0 + ((1.0 - ((1.0 * l.price) / :budget)) * 40.0)
            end) * rs.budgetWeight)
          end desc,
          l.price asc,
          l.id asc
        """,
        countQuery = """
        select count(p)
        from LaptopProfile p
        join p.laptop l
        join RecommendationScore rs on rs.laptop = l
        where rs.useCase = :useCase
          and rs.gateScore >= :gateThreshold
          and l.price is not null
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
        """,
    )
    fun findRecommendationCandidatePage(
        @Param("maxPrice") maxPrice: Int,
        @Param("maxWeight") maxWeight: Double,
        @Param("screenSizes") screenSizes: Collection<Int>,
        @Param("screenFilterEnabled") screenFilterEnabled: Boolean,
        @Param("includeUnknownScreen") includeUnknownScreen: Boolean,
        @Param("gateThreshold") gateThreshold: Int,
        @Param("budget") budget: Int,
        @Param("useCase") useCase: String,
        @Param("sortMode") sortMode: String,
        pageable: Pageable,
    ): Page<LaptopProfile>
}
