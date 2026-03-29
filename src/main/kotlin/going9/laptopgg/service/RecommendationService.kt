package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.repository.LaptopProfileRepository
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.RecommendationUseCase
import going9.laptopgg.dto.request.ScreenSizeMode
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationService(
    private val laptopProfileRepository: LaptopProfileRepository,
    private val laptopProfileService: LaptopProfileService,
    private val scoreCalculatorService: ScoreCalculatorService,
) {
    @Transactional
    fun recommendLaptops(request: LaptopRecommendationRequest, pageable: Pageable): Page<LaptopRecommendationListResponse> {
        laptopProfileService.syncMissingProfilesIfNeeded()

        val useCase = request.resolvedUseCase()

        val candidates = findCandidates(request)
            .filter { profile -> matchesBaseFilters(profile.laptop, request) }
            .map { profile ->
                val gateScore = scoreCalculatorService.gateScore(profile, useCase)
                profile to gateScore
            }
            .filter { (_, gateScore) -> gateScore >= scoreCalculatorService.gateThreshold(useCase) }
            .map { (profile, gateScore) ->
                val scoreResult = scoreCalculatorService.calculateScore(profile.laptop, profile, request)
                ScoredLaptop(
                    laptop = profile.laptop,
                    gateScore = gateScore,
                    score = scoreResult.score,
                    reasons = scoreResult.reasons,
                )
            }

        val sortedCandidates = sortCandidates(candidates, pageable)
        val pageContent = paginate(sortedCandidates, pageable)

        return PageImpl(
            pageContent.map { candidate ->
                LaptopRecommendationListResponse(
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
            },
            pageable,
            candidates.size.toLong(),
        )
    }

    private fun findCandidates(request: LaptopRecommendationRequest) =
        when (request.resolvedScreenSizeMode()) {
            ScreenSizeMode.SELECT -> laptopProfileRepository.findRecommendationCandidatesByScreenSizes(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
                screenSizes = request.normalizedScreenSizes(),
            )
            else -> laptopProfileRepository.findRecommendationCandidates(
                maxPrice = request.budget,
                maxWeight = request.maxWeightKg,
            )
        }

    private fun matchesBaseFilters(
        laptop: Laptop,
        request: LaptopRecommendationRequest,
    ): Boolean {
        if (!request.matchesScreenSize(laptop.screenSize)) {
            return false
        }

        return true
    }

    private fun sortCandidates(
        candidates: List<ScoredLaptop>,
        pageable: Pageable,
    ): List<ScoredLaptop> {
        if (!pageable.sort.isSorted) {
            return candidates.sortedWith(
                compareByDescending<ScoredLaptop> { it.score }
                    .thenBy { it.laptop.price ?: Int.MAX_VALUE }
                    .thenBy { it.laptop.id ?: Long.MAX_VALUE },
            )
        }

        val orders = pageable.sort.toList()
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

    private fun paginate(candidates: List<ScoredLaptop>, pageable: Pageable): List<ScoredLaptop> {
        val startIndex = pageable.offset.toInt().coerceAtMost(candidates.size)
        val endIndex = (startIndex + pageable.pageSize).coerceAtMost(candidates.size)
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
    }
}
