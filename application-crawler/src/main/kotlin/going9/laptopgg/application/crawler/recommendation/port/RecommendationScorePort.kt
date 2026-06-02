package going9.laptopgg.application.crawler.recommendation.port

import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand

interface RecommendationScorePort {
    fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>)
}
