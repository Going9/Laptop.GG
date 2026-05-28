package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.port.out.LaptopProfilePort
import going9.laptopgg.application.port.out.RecommendationCandidateRecord
import going9.laptopgg.application.port.out.RecommendationCandidateFilter
import going9.laptopgg.application.port.out.RecommendationCandidatePageQuery
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopProfileRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class LaptopProfileJpaAdapter(
    private val laptopProfileRepository: WebLaptopProfileRepository,
) : LaptopProfilePort {
    override fun findRecommendationCandidates(filter: RecommendationCandidateFilter): List<RecommendationCandidateRecord> {
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
        ).map { it.toRecommendationCandidateRecord() }
    }

    override fun findRecommendationCandidatePage(query: RecommendationCandidatePageQuery): PagedResult<RecommendationCandidateRecord> {
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
            content = page.content.map { it.toRecommendationCandidateRecord() },
            page = query.pageQuery.page,
            size = query.pageQuery.size,
            totalElements = page.totalElements,
            sort = query.pageQuery.sort,
        )
    }

    private fun LaptopProfile.toRecommendationCandidateRecord(): RecommendationCandidateRecord {
        val laptopId = requireNotNull(laptop.id) { "Persisted laptop id must not be null." }
        val laptopPrice = requireNotNull(laptop.price) { "Recommendation candidate price must not be null." }
        return RecommendationCandidateRecord(
            id = laptopId,
            name = laptop.name,
            imageUrl = laptop.imageUrl,
            price = laptopPrice,
            weight = laptop.weight,
            screenSize = laptop.screenSize,
            cpu = laptop.cpu,
            graphicsType = laptop.graphicsType,
            resolution = laptop.resolution,
            portabilityScore = portabilityScore,
            displayScore = displayScore,
            ramScore = ramScore,
            tgpScore = tgpScore,
            cpuPerformanceScore = cpuPerformanceScore,
            lowPowerCpuScore = lowPowerCpuScore,
            gpuPerformanceScore = gpuPerformanceScore,
            gpuCreatorBonus = gpuCreatorBonus,
            officeScore = officeScore,
            batteryScore = batteryScore,
            casualGameScore = casualGameScore,
            onlineGameScore = onlineGameScore,
            aaaGameScore = aaaGameScore,
            creatorScore = creatorScore,
        )
    }
}
