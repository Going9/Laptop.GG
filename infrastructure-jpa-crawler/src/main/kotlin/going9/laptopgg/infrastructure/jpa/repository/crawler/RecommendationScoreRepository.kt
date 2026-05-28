package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import org.springframework.data.jpa.repository.JpaRepository

interface RecommendationScoreRepository : JpaRepository<RecommendationScore, Long> {
    fun findAllByLaptopId(laptopId: Long): List<RecommendationScore>
}
