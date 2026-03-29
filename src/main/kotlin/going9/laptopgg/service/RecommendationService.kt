package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.domain.repository.LaptopProfileRepository
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.RecommendationUseCase
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
        laptopProfileService.syncMissingProfiles()

        val useCase = request.resolvedUseCase()

        val candidates = laptopProfileRepository.findAllWithLaptopAndUsage()
            .filter { profile -> matchesBaseFilters(profile.laptop, request) }
            .filter { profile -> scoreCalculatorService.gateScore(profile, useCase) >= scoreCalculatorService.gateThreshold(useCase) }
            .map { profile ->
                val scoreResult = scoreCalculatorService.calculateScore(profile.laptop, profile, request)
                ScoredLaptop(
                    laptop = profile.laptop,
                    profile = profile,
                    score = scoreResult.score,
                    reasons = scoreResult.reasons,
                )
            }

        val sortedCandidates = sortCandidates(candidates, pageable, useCase)
        val pageContent = paginate(sortedCandidates, pageable)

        return PageImpl(
            pageContent.map { candidate ->
                LaptopRecommendationListResponse(
                    id = candidate.laptop.id!!,
                    score = candidate.score,
                    imgLink = candidate.laptop.imageUrl,
                    price = candidate.laptop.price!!,
                    name = candidate.laptop.name,
                    manufacturer = candidate.laptop.name.substringBefore(" "),
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

    private fun matchesBaseFilters(
        laptop: Laptop,
        request: LaptopRecommendationRequest,
    ): Boolean {
        val price = laptop.price ?: return false

        if (price > request.budget) {
            return false
        }
        if (!request.matchesScreenSize(laptop.screenSize)) {
            return false
        }

        return laptop.weight == null || laptop.weight!! <= request.maxWeightKg
    }

    private fun sortCandidates(
        candidates: List<ScoredLaptop>,
        pageable: Pageable,
        useCase: RecommendationUseCase,
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
                    "weight" -> compareNullableWeight(left.laptop.weight, right.laptop.weight)
                    "recommended" -> right.score.compareTo(left.score)
                    else -> 0
                }

                if (comparison != 0) {
                    return@Comparator if (order.isAscending) comparison else -comparison
                }
            }

            val scoreComparison = right.score.compareTo(left.score)
            if (scoreComparison != 0) {
                return@Comparator scoreComparison
            }

            val gateComparison = scoreCalculatorService.gateScore(right.profile, useCase)
                .compareTo(scoreCalculatorService.gateScore(left.profile, useCase))
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

    private fun compareNullableWeight(left: Double?, right: Double?): Int {
        return when {
            left == null && right == null -> 0
            left == null -> 1
            right == null -> -1
            else -> left.compareTo(right)
        }
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
        val profile: LaptopProfile,
        val score: Double,
        val reasons: List<String>,
    )

    companion object {
        private val RESOLUTION_REGEX = Regex("""(\d{3,4})\s*[xX]\s*(\d{3,4})""")
    }
}
