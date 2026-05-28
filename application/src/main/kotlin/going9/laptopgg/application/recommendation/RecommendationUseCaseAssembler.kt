package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort

object RecommendationUseCaseAssembler {
    fun createRecommendLaptopsUseCase(
        recommendationCandidatePort: RecommendationCandidatePort,
        transactionPort: ApplicationTransactionPort,
    ): RecommendLaptopsUseCase {
        val recommendationReasonBuilder = RecommendationReasonBuilder()
        return DefaultRecommendLaptopsUseCase(
            recommendationCandidatePort = recommendationCandidatePort,
            recommendationScoreCalculator = RecommendationScoreCalculator(recommendationReasonBuilder),
            candidateFilterFactory = RecommendationCandidateFilterFactory(),
            sortModeResolver = RecommendationSortModeResolver(),
            resultMapper = LaptopRecommendationResultMapper(),
            queryValidator = LaptopRecommendationQueryValidator(),
            transactionPort = transactionPort,
        )
    }
}
