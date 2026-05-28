package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.port.out.RecommendationScorePort
import going9.laptopgg.domain.recommendation.RecommendationScore
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import org.springframework.stereotype.Component

@Component
class RecommendationScoreJpaAdapter(
    private val recommendationScoreRepository: RecommendationScoreRepository,
) : RecommendationScorePort {
    override fun findAllByLaptopId(laptopId: Long): List<RecommendationScore> {
        return recommendationScoreRepository.findAllByLaptopId(laptopId)
    }

    override fun saveAll(scores: Iterable<RecommendationScore>): List<RecommendationScore> {
        return recommendationScoreRepository.saveAll(scores)
    }
}
