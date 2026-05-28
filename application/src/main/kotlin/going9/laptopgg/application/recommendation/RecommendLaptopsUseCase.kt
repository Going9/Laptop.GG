package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.port.out.LaptopProfilePort
import going9.laptopgg.application.port.out.RecommendationCandidateFilter
import going9.laptopgg.application.port.out.RecommendationCandidatePageQuery
import going9.laptopgg.application.port.out.RecommendationCandidateRecord
import going9.laptopgg.recommendation.RecommendationUseCase
import kotlin.math.ceil

class RecommendLaptopsUseCase(
    private val laptopProfilePort: LaptopProfilePort,
    private val recommendationScoreCalculator: RecommendationScoreCalculator,
) {
    fun recommend(request: LaptopRecommendationQuery, pageQuery: PageQuery): PagedResult<LaptopRecommendationResult> {
        val useCase = request.resolvedUseCase()
        val candidateFilter = buildCandidateFilter(request, useCase)
        val sortMode = resolveSortMode(pageQuery)

        val candidatePage = if (sortMode != null) {
            findCandidatePage(request, candidateFilter, useCase, sortMode, pageQuery)
        } else {
            val candidates = findCandidates(candidateFilter)
                .map { candidate -> scoreCandidate(candidate, request, useCase) }

            val sortedCandidates = sortCandidates(candidates, pageQuery)
            val pageContent = paginate(sortedCandidates, pageQuery)
            return PagedResult(
                content = pageContent.map(::toResponse),
                page = pageQuery.page,
                size = pageQuery.size,
                totalElements = candidates.size.toLong(),
                sort = pageQuery.sort,
            )
        }

        val pageContent = candidatePage.content.map { profile ->
            scoreCandidate(profile, request, useCase)
        }

        return PagedResult(
            content = pageContent.map(::toResponse),
            page = pageQuery.page,
            size = pageQuery.size,
            totalElements = candidatePage.totalElements,
            sort = pageQuery.sort,
        )
    }

    private fun scoreCandidate(
        candidate: RecommendationCandidateRecord,
        request: LaptopRecommendationQuery,
        useCase: RecommendationUseCase,
    ): ScoredLaptop {
        val scoreResult = recommendationScoreCalculator.calculateScore(candidate, request)
        return ScoredLaptop(
            candidate = candidate,
            gateScore = recommendationScoreCalculator.gateScore(candidate, useCase),
            score = scoreResult.score,
            reasons = scoreResult.reasons,
        )
    }

    private fun toResponse(candidate: ScoredLaptop): LaptopRecommendationResult {
        return LaptopRecommendationResult(
            id = candidate.candidate.id,
            score = candidate.score,
            imgLink = candidate.candidate.imageUrl,
            price = candidate.candidate.price,
            name = candidate.candidate.name,
            manufacturer = manufacturerName(candidate.candidate.name),
            weight = candidate.candidate.weight,
            screenSize = candidate.candidate.screenSize,
            cpu = candidate.candidate.cpu,
            gpu = candidate.candidate.graphicsType,
            resolutionLabel = resolutionLabel(candidate.candidate.resolution),
            reasons = candidate.reasons,
        )
    }

    private fun findCandidates(
        candidateFilter: RecommendationCandidateFilter,
    ) = laptopProfilePort.findRecommendationCandidates(candidateFilter)

    private fun findCandidatePage(
        request: LaptopRecommendationQuery,
        candidateFilter: RecommendationCandidateFilter,
        useCase: RecommendationUseCase,
        sortMode: String,
        pageQuery: PageQuery,
    ) = laptopProfilePort.findRecommendationCandidatePage(
        RecommendationCandidatePageQuery(
            filter = candidateFilter,
            gateThreshold = recommendationScoreCalculator.gateThreshold(useCase),
            budget = request.budget,
            useCase = useCase.name,
            sortMode = sortMode,
            pageQuery = pageQuery,
        ),
    )

    private fun buildCandidateFilter(
        request: LaptopRecommendationQuery,
        useCase: RecommendationUseCase,
    ): RecommendationCandidateFilter {
        val gateThreshold = recommendationScoreCalculator.gateThreshold(useCase)
        val screenMode = request.resolvedScreenSizeMode()

        val baseFilter = when (screenMode) {
            ScreenSizeMode.SELECT -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = false,
                screenSizes = request.normalizedScreenSizes(),
            )
            ScreenSizeMode.ANY -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = false,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationQuery.ALL_SELECTABLE_SCREEN_SIZES,
            )
            ScreenSizeMode.NOT_SURE -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationQuery.COMMON_SCREEN_SIZES,
            )
        }

        return when (useCase) {
            RecommendationUseCase.NOT_SURE -> baseFilter.copy(
                minNotSureGateTotal = minimumRoundedAverageTotal(gateThreshold, 3),
            )
            RecommendationUseCase.OFFICE_STUDY,
            RecommendationUseCase.PORTABLE_OFFICE,
            -> baseFilter.copy(minOfficeScore = gateThreshold)
            RecommendationUseCase.BATTERY_FIRST -> baseFilter.copy(minBatteryScore = gateThreshold)
            RecommendationUseCase.CASUAL_GAME -> baseFilter.copy(minCasualGameScore = gateThreshold)
            RecommendationUseCase.ONLINE_GAME -> baseFilter.copy(minOnlineGameScore = gateThreshold)
            RecommendationUseCase.AAA_GAME -> baseFilter.copy(minAaaGameScore = gateThreshold)
            RecommendationUseCase.CREATOR -> baseFilter.copy(minCreatorScore = gateThreshold)
        }
    }

    private fun sortCandidates(
        candidates: List<ScoredLaptop>,
        pageQuery: PageQuery,
    ): List<ScoredLaptop> {
        if (pageQuery.sort.isEmpty()) {
            return candidates.sortedWith(
                compareByDescending<ScoredLaptop> { it.score }
                    .thenBy { it.candidate.price }
                    .thenBy { it.candidate.id },
            )
        }

        val orders = pageQuery.sort
        return candidates.sortedWith(Comparator { left, right ->
            for (order in orders) {
                val comparison = when (order.property) {
                    "price" -> compareValues(left.candidate.price, right.candidate.price)
                    "weight" -> compareWeight(left.candidate.weight, right.candidate.weight, order.isAscending)
                    "recommended" -> right.score.compareTo(left.score)
                    else -> 0
                }

                if (comparison != 0) {
                    return@Comparator when (order.property) {
                        "weight", "recommended" -> comparison
                        else -> if (order.isAscending) comparison else -comparison
                    }
                }
            }

            val scoreComparison = right.score.compareTo(left.score)
            if (scoreComparison != 0) {
                return@Comparator scoreComparison
            }

            val gateComparison = right.gateScore.compareTo(left.gateScore)
            if (gateComparison != 0) {
                return@Comparator gateComparison
            }

            compareValues(left.candidate.id, right.candidate.id)
        })
    }

    private fun paginate(candidates: List<ScoredLaptop>, pageQuery: PageQuery): List<ScoredLaptop> {
        val startIndex = pageQuery.offset.coerceAtMost(candidates.size)
        val endIndex = (startIndex + pageQuery.size).coerceAtMost(candidates.size)
        if (startIndex >= endIndex) {
            return emptyList()
        }

        return candidates.subList(startIndex, endIndex)
    }

    private fun compareWeight(left: Double?, right: Double?, ascending: Boolean): Int {
        return when {
            left == null && right == null -> 0
            left == null -> 1
            right == null -> -1
            ascending -> left.compareTo(right)
            else -> right.compareTo(left)
        }
    }

    private fun manufacturerName(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "브랜드 확인 불가"
    }

    private fun resolutionLabel(resolution: String?): String? {
        val raw = resolution?.trim().orEmpty()
        if (raw.isBlank()) {
            return null
        }

        val normalized = raw.uppercase()
        return when {
            normalized.contains("UHD") || normalized.contains("4K") -> "UHD"
            normalized.contains("QHD") || normalized.contains("WQHD") || normalized.contains("WQXGA") -> "QHD"
            normalized.contains("FHD") || normalized.contains("WUXGA") -> "FHD"
            normalized.contains("HD") -> "HD"
            else -> {
                val match = RESOLUTION_REGEX.find(normalized) ?: return raw
                val width = match.groupValues[1].toIntOrNull() ?: return raw
                when {
                    width >= 3840 -> "UHD"
                    width >= 2560 -> "QHD"
                    width >= 1920 -> "FHD"
                    width >= 1280 -> "HD"
                    else -> raw
                }
            }
        }
    }

    private data class ScoredLaptop(
        val candidate: RecommendationCandidateRecord,
        val gateScore: Int,
        val score: Double,
        val reasons: List<String>,
    )

    companion object {
        private val RESOLUTION_REGEX = Regex("""(\d{3,4})\s*[xX]\s*(\d{3,4})""")

        private fun minimumRoundedAverageTotal(threshold: Int, componentCount: Int): Int {
            require(componentCount > 0) { "componentCount must be greater than zero." }
            return ceil((threshold - 0.5) * componentCount).toInt()
        }

        private fun resolveSortMode(pageQuery: PageQuery): String? {
            if (pageQuery.sort.isEmpty()) {
                return "recommended"
            }

            val firstOrder = pageQuery.sort.firstOrNull() ?: return "recommended"
            return when (firstOrder.property) {
                "recommended" -> "recommended"
                "price" -> if (firstOrder.isAscending) "price_asc" else "price_desc"
                "weight" -> if (firstOrder.isAscending) "weight_asc" else "weight_desc"
                else -> null
            }
        }
    }
}
