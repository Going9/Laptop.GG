package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
internal class RecommendationScoreJpaAdapter(
    private val recommendationScoreRepository: RecommendationScoreRepository,
    private val entityManager: EntityManager,
) : RecommendationScorePort {
    override fun saveAll(scores: Iterable<UpsertRecommendationScoreCommand>) {
        val commands = scores.toList()
        if (commands.isEmpty()) {
            return
        }

        val laptopIds = commands.map { it.laptopId }.distinct()
        if (laptopIds.size != 1) {
            throw CrawlerInvalidCommandException("Recommendation scores must target a single laptop.")
        }

        val laptopId = laptopIds.single()
        commands.forEach { command ->
            val useCase = command.useCase.name
            val updatedRows = recommendationScoreRepository.updateByLaptopIdAndUseCase(
                laptopId = laptopId,
                useCase = useCase,
                gateScore = command.gateScore,
                staticScore = command.staticScore,
                budgetWeight = command.budgetWeight,
                updatedAt = command.updatedAt,
            )
            if (updatedRows == 0) {
                recommendationScoreRepository.save(
                    RecommendationScore(
                        laptop = entityManager.getReference(Laptop::class.java, laptopId),
                        useCase = useCase,
                        gateScore = command.gateScore,
                        staticScore = command.staticScore,
                        budgetWeight = command.budgetWeight,
                        updatedAt = command.updatedAt,
                    ),
                )
            }
        }
    }
}
