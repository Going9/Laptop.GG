package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand

interface RecommendationScorePort {
    fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>)
}
