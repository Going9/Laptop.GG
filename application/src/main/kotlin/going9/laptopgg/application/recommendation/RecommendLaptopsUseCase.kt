package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.port.RecommendationCandidateFilter
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePageQuery
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import going9.laptopgg.application.recommendation.port.RecommendationCandidateSortMode
import going9.laptopgg.recommendation.RecommendationUseCase

class RecommendLaptopsUseCase(
    private val recommendationCandidatePort: RecommendationCandidatePort,
    private val recommendationScoreCalculator: RecommendationScoreCalculator,
    private val candidateFilterFactory: RecommendationCandidateFilterFactory,
    private val sortModeResolver: RecommendationSortModeResolver,
    private val resultMapper: LaptopRecommendationResultMapper,
) {
    fun recommend(request: LaptopRecommendationQuery, pageQuery: PageQuery): PagedResult<LaptopRecommendationResult> {
        val useCase = request.resolvedUseCase()
        val gateThreshold = recommendationScoreCalculator.gateThreshold(useCase)
        val candidateFilter = candidateFilterFactory.create(request, useCase, gateThreshold)
        val sortMode = sortModeResolver.resolve(pageQuery)

        val candidatePage = findCandidatePage(request, candidateFilter, gateThreshold, useCase, sortMode, pageQuery)
        val pageContent = candidatePage.content.map { profile ->
            val scoreResult = recommendationScoreCalculator.calculateScore(profile, request)
            resultMapper.toResult(profile, scoreResult)
        }

        return PagedResult(
            content = pageContent,
            page = pageQuery.page,
            size = pageQuery.size,
            totalElements = candidatePage.totalElements,
            sort = pageQuery.sort,
        )
    }

    private fun findCandidatePage(
        request: LaptopRecommendationQuery,
        candidateFilter: RecommendationCandidateFilter,
        gateThreshold: Int,
        useCase: RecommendationUseCase,
        sortMode: RecommendationCandidateSortMode,
        pageQuery: PageQuery,
    ) = recommendationCandidatePort.findRecommendationCandidatePage(
        RecommendationCandidatePageQuery(
            filter = candidateFilter,
            gateThreshold = gateThreshold,
            budget = request.budget,
            useCase = useCase.name,
            sortMode = sortMode,
            pageQuery = pageQuery,
        ),
    )
}
