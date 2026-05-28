package going9.laptopgg.service

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.port.out.LaptopProfilePort
import going9.laptopgg.application.port.out.RecommendationCandidateFilter
import going9.laptopgg.application.port.out.RecommendationCandidatePageQuery
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.RecommendationUseCase
import going9.laptopgg.dto.request.ScreenSizeMode
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class RecommendationService(
    private val laptopProfilePort: LaptopProfilePort,
    private val scoreCalculatorService: ScoreCalculatorService,
) {
    fun recommendLaptops(request: LaptopRecommendationRequest, pageQuery: PageQuery): PagedResult<LaptopRecommendationListResponse> {
        val useCase = request.resolvedUseCase()
        val candidateFilter = buildCandidateFilter(request, useCase)
        val sortMode = resolveSortMode(pageQuery)

        val candidatePage = if (sortMode != null) {
            findCandidatePage(request, candidateFilter, useCase, sortMode, pageQuery)
        } else {
            val candidates = findCandidates(candidateFilter)
                .map { profile -> scoreCandidate(profile.laptop, profile, request, useCase) }

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
            scoreCandidate(profile.laptop, profile, request, useCase)
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
        laptop: Laptop,
        profile: LaptopProfile,
        request: LaptopRecommendationRequest,
        useCase: RecommendationUseCase,
    ): ScoredLaptop {
        val scoreResult = scoreCalculatorService.calculateScore(laptop, profile, request)
        return ScoredLaptop(
            laptop = laptop,
            gateScore = scoreCalculatorService.gateScore(profile, useCase),
            score = scoreResult.score,
            reasons = scoreResult.reasons,
        )
    }

    private fun toResponse(candidate: ScoredLaptop): LaptopRecommendationListResponse {
        return LaptopRecommendationListResponse(
            id = candidate.laptop.id!!,
            score = candidate.score,
            imgLink = candidate.laptop.imageUrl,
            price = candidate.laptop.price!!,
            name = candidate.laptop.name,
            manufacturer = manufacturerName(candidate.laptop.name),
            weight = candidate.laptop.weight,
            screenSize = candidate.laptop.screenSize,
            cpu = candidate.laptop.cpu,
            gpu = candidate.laptop.graphicsType,
            resolutionLabel = resolutionLabel(candidate.laptop.resolution),
            reasons = candidate.reasons,
        )
    }

    private fun findCandidates(
        candidateFilter: RecommendationCandidateFilter,
    ) = laptopProfilePort.findRecommendationCandidates(candidateFilter)

    private fun findCandidatePage(
        request: LaptopRecommendationRequest,
        candidateFilter: RecommendationCandidateFilter,
        useCase: RecommendationUseCase,
        sortMode: String,
        pageQuery: PageQuery,
    ) = laptopProfilePort.findRecommendationCandidatePage(
        RecommendationCandidatePageQuery(
            filter = candidateFilter,
            gateThreshold = scoreCalculatorService.gateThreshold(useCase),
            budget = request.budget,
            useCase = useCase.name,
            sortMode = sortMode,
            pageQuery = pageQuery,
        ),
    )

    private fun buildCandidateFilter(
        request: LaptopRecommendationRequest,
        useCase: RecommendationUseCase,
    ): RecommendationCandidateFilter {
        val gateThreshold = scoreCalculatorService.gateThreshold(useCase)
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
                screenSizes = LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES,
            )
            ScreenSizeMode.NOT_SURE -> RecommendationCandidateFilter(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenFilterEnabled = true,
                includeUnknownScreen = true,
                screenSizes = LaptopRecommendationRequest.COMMON_SCREEN_SIZES,
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
                    .thenBy { it.laptop.price ?: Int.MAX_VALUE }
                    .thenBy { it.laptop.id ?: Long.MAX_VALUE },
            )
        }

        val orders = pageQuery.sort
        return candidates.sortedWith(Comparator { left, right ->
            for (order in orders) {
                val comparison = when (order.property) {
                    "price" -> compareValues(left.laptop.price, right.laptop.price)
                    "weight" -> compareWeight(left.laptop.weight, right.laptop.weight, order.isAscending)
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

            compareValues(left.laptop.id, right.laptop.id)
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
        val laptop: Laptop,
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
