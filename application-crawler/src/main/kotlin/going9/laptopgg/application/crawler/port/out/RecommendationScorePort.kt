package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.domain.recommendation.RecommendationScore

interface RecommendationScorePort {
    fun findAllByLaptopId(laptopId: Long): List<RecommendationScore>
    fun saveAll(scores: Iterable<RecommendationScore>): List<RecommendationScore>
}
