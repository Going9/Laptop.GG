package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface RecommendationScoreRepository : JpaRepository<RecommendationScore, Long> {
    @Modifying(flushAutomatically = true)
    @Query(
        """
        update RecommendationScore rs
        set rs.gateScore = :gateScore,
            rs.staticScore = :staticScore,
            rs.budgetWeight = :budgetWeight,
            rs.updatedAt = :updatedAt
        where rs.laptop.id = :laptopId
          and rs.useCase = :useCase
        """,
    )
    fun updateByLaptopIdAndUseCase(
        @Param("laptopId") laptopId: Long,
        @Param("useCase") useCase: String,
        @Param("gateScore") gateScore: Int,
        @Param("staticScore") staticScore: Double,
        @Param("budgetWeight") budgetWeight: Double,
        @Param("updatedAt") updatedAt: LocalDateTime,
    ): Int
}
