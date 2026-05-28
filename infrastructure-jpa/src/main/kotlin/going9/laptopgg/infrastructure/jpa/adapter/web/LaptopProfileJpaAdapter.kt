package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.port.out.LaptopProfilePort
import going9.laptopgg.application.port.out.RecommendationCandidateFilter
import going9.laptopgg.application.port.out.RecommendationCandidatePageQuery
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.infrastructure.jpa.repository.shared.LaptopProfileRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class LaptopProfileJpaAdapter(
    private val laptopProfileRepository: LaptopProfileRepository,
) : LaptopProfilePort {
    override fun findRecommendationCandidates(filter: RecommendationCandidateFilter): List<LaptopProfile> {
        return laptopProfileRepository.findRecommendationCandidates(
            maxPrice = filter.maxPrice,
            maxWeight = filter.maxWeight,
            screenSizes = filter.screenSizes,
            screenFilterEnabled = filter.screenFilterEnabled,
            includeUnknownScreen = filter.includeUnknownScreen,
            minOfficeScore = filter.minOfficeScore,
            minBatteryScore = filter.minBatteryScore,
            minCasualGameScore = filter.minCasualGameScore,
            minOnlineGameScore = filter.minOnlineGameScore,
            minAaaGameScore = filter.minAaaGameScore,
            minCreatorScore = filter.minCreatorScore,
            minNotSureGateTotal = filter.minNotSureGateTotal,
        )
    }

    override fun findRecommendationCandidatePage(query: RecommendationCandidatePageQuery): PagedResult<LaptopProfile> {
        val page = laptopProfileRepository.findRecommendationCandidatePage(
            maxPrice = query.filter.maxPrice,
            maxWeight = query.filter.maxWeight,
            screenSizes = query.filter.screenSizes,
            screenFilterEnabled = query.filter.screenFilterEnabled,
            includeUnknownScreen = query.filter.includeUnknownScreen,
            gateThreshold = query.gateThreshold,
            budget = query.budget,
            useCase = query.useCase,
            sortMode = query.sortMode,
            pageable = PageRequest.of(query.pageQuery.page, query.pageQuery.size),
        )

        return PagedResult(
            content = page.content,
            page = query.pageQuery.page,
            size = query.pageQuery.size,
            totalElements = page.totalElements,
            sort = query.pageQuery.sort,
        )
    }
}
