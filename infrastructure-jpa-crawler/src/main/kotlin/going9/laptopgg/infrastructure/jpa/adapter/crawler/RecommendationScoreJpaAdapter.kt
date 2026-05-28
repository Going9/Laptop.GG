package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import org.springframework.stereotype.Component

@Component
internal class RecommendationScoreJpaAdapter(
    private val crawlerLaptopRepository: CrawlerLaptopRepository,
    private val recommendationScoreRepository: RecommendationScoreRepository,
) : RecommendationScorePort {
    override fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>) {
        val commands = scores.toList()
        if (commands.isEmpty()) {
            return
        }

        val laptopIds = commands.map { it.laptopId }.distinct()
        require(laptopIds.size == 1) { "Recommendation scores must target a single laptop." }

        val laptopId = laptopIds.single()
        val laptopReference = crawlerLaptopRepository.getReferenceById(laptopId)
        val existingScores = recommendationScoreRepository.findAllByLaptopId(laptopId)
            .associateBy { it.useCase }
        val entities = commands.map { command ->
            val score = existingScores[command.useCase] ?: RecommendationScore(
                laptop = laptopReference,
                useCase = command.useCase,
                gateScore = 0,
                staticScore = 0.0,
                budgetWeight = 0.0,
                updatedAt = command.updatedAt,
            )
            score.gateScore = command.gateScore
            score.staticScore = command.staticScore
            score.budgetWeight = command.budgetWeight
            score.updatedAt = command.updatedAt
            score
        }

        recommendationScoreRepository.saveAll(entities)
    }
}
