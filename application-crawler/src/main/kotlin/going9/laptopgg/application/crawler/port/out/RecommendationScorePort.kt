package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.UpsertRecommendationScoreCommand

interface RecommendationScorePort {
    fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>)
}
