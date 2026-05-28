package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import going9.laptopgg.application.recommendation.port.RecommendationCandidateRecord
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePageQuery
import going9.laptopgg.persistence.model.laptop.LaptopProfile
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopProfileRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
internal class RecommendationCandidateJpaAdapter(
    private val laptopProfileRepository: WebLaptopProfileRepository,
) : RecommendationCandidatePort {
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
            sortMode = query.sortMode.queryValue,
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
