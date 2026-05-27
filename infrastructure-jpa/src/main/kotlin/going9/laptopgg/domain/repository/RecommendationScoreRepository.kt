package going9.laptopgg.domain.repository

import going9.laptopgg.domain.recommendation.RecommendationScore
import org.springframework.data.jpa.repository.JpaRepository

interface RecommendationScoreRepository : JpaRepository<RecommendationScore, Long> {
    fun findAllByLaptopId(laptopId: Long): List<RecommendationScore>
}
