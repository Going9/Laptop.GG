package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.recommendation.port.RecommendationCandidateFilter
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePageQuery
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import going9.laptopgg.application.recommendation.port.RecommendationCandidateSortMode
import going9.laptopgg.recommendation.RecommendationUseCase

interface RecommendLaptopsUseCase {
    fun recommend(request: LaptopRecommendationQuery, pageQuery: PageQuery): PagedResult<LaptopRecommendationResult>
}

internal class DefaultRecommendLaptopsUseCase(
    private val recommendationCandidatePort: RecommendationCandidatePort,
    private val recommendationScoreCalculator: RecommendationScoreCalculator,
    private val candidateFilterFactory: RecommendationCandidateFilterFactory,
    private val sortModeResolver: RecommendationSortModeResolver,
    private val resultMapper: LaptopRecommendationResultMapper,
    private val queryValidator: LaptopRecommendationQueryValidator,
    private val transactionPort: ApplicationTransactionPort,
) : RecommendLaptopsUseCase {
    override fun recommend(request: LaptopRecommendationQuery, pageQuery: PageQuery): PagedResult<LaptopRecommendationResult> {
        queryValidator.validate(request)
        validatePageQuery(pageQuery)
        return transactionPort.read {
            recommendInTransaction(request, pageQuery)
        }
    }

    private fun recommendInTransaction(
        request: LaptopRecommendationQuery,
        pageQuery: PageQuery,
    ): PagedResult<LaptopRecommendationResult> {
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
            useCase = useCase,
            sortMode = sortMode,
            pageQuery = pageQuery,
        ),
    )

    private fun validatePageQuery(pageQuery: PageQuery) {
        if (pageQuery.page < 0) {
            throw InvalidCommandException("page must not be negative.")
        }
        if (pageQuery.size <= 0) {
            throw InvalidCommandException("size must be positive.")
        }
        if (pageQuery.size > PageQuery.MAX_SIZE) {
            throw InvalidCommandException("size must be ${PageQuery.MAX_SIZE} or less.")
        }
    }
}
