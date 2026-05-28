package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.recommendation.port.RecommendationCandidateFilter
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePageQuery
import going9.laptopgg.application.recommendation.port.RecommendationCandidateSortMode
import going9.laptopgg.infrastructure.jpa.repository.web.RecommendationCandidateProjection
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopProfileRepository
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class RecommendationCandidateJpaAdapterStateTest {
    @Test
    fun `findRecommendationCandidatePage maps projection without loading profile entity`() {
        val repository = Mockito.mock(WebLaptopProfileRepository::class.java)
        val adapter = RecommendationCandidateJpaAdapter(repository)
        val pageRequest = PageRequest.of(0, 10)
        val query = recommendationQuery()
        Mockito.`when`(
            repository.findRecommendationCandidatePage(
                maxPrice = 2_000_000,
                maxWeight = 2.0,
                screenSizes = listOf(14, 16),
                screenFilterEnabled = true,
                includeUnknownScreen = false,
                gateThreshold = 60,
                budget = 2_000_000,
                useCase = RecommendationUseCase.OFFICE_STUDY.name,
                sortMode = RecommendationCandidateSortMode.RECOMMENDED.queryValue,
                pageable = pageRequest,
            ),
        ).thenReturn(PageImpl(listOf(recommendationProjection()), pageRequest, 1))

        val result = adapter.findRecommendationCandidatePage(query)

        assertThat(result.content).hasSize(1)
        assertThat(result.content.single().id).isEqualTo(1L)
        assertThat(result.content.single().name).isEqualTo("Laptop")
        assertThat(result.content.single().price).isEqualTo(1_200_000)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `findRecommendationCandidatePage rejects projected candidate without laptop id`() {
        val repository = Mockito.mock(WebLaptopProfileRepository::class.java)
        val adapter = RecommendationCandidateJpaAdapter(repository)
        val pageRequest = PageRequest.of(0, 10)
        val query = recommendationQuery()
        Mockito.`when`(
            repository.findRecommendationCandidatePage(
                maxPrice = 2_000_000,
                maxWeight = 2.0,
                screenSizes = listOf(14, 16),
                screenFilterEnabled = true,
                includeUnknownScreen = false,
                gateThreshold = 60,
                budget = 2_000_000,
                useCase = RecommendationUseCase.OFFICE_STUDY.name,
                sortMode = RecommendationCandidateSortMode.RECOMMENDED.queryValue,
                pageable = pageRequest,
            ),
        ).thenReturn(PageImpl(listOf(recommendationProjection(laptopId = null)), pageRequest, 1))

        assertThatThrownBy {
            adapter.findRecommendationCandidatePage(query)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    @Test
    fun `findRecommendationCandidatePage rejects projected candidate without price`() {
        val repository = Mockito.mock(WebLaptopProfileRepository::class.java)
        val adapter = RecommendationCandidateJpaAdapter(repository)
        val pageRequest = PageRequest.of(0, 10)
        val query = recommendationQuery()
        Mockito.`when`(
            repository.findRecommendationCandidatePage(
                maxPrice = 2_000_000,
                maxWeight = 2.0,
                screenSizes = listOf(14, 16),
                screenFilterEnabled = true,
                includeUnknownScreen = false,
                gateThreshold = 60,
                budget = 2_000_000,
                useCase = RecommendationUseCase.OFFICE_STUDY.name,
                sortMode = RecommendationCandidateSortMode.RECOMMENDED.queryValue,
                pageable = pageRequest,
            ),
        ).thenReturn(PageImpl(listOf(recommendationProjection(price = null)), pageRequest, 1))

        assertThatThrownBy {
            adapter.findRecommendationCandidatePage(query)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    private fun recommendationQuery(): RecommendationCandidatePageQuery {
        return RecommendationCandidatePageQuery(
            filter = RecommendationCandidateFilter(
                maxPrice = 2_000_000,
                maxWeight = 2.0,
                screenSizes = listOf(14, 16),
                screenFilterEnabled = true,
                includeUnknownScreen = false,
            ),
            gateThreshold = 60,
            budget = 2_000_000,
            useCase = RecommendationUseCase.OFFICE_STUDY,
            sortMode = RecommendationCandidateSortMode.RECOMMENDED,
            pageQuery = PageQuery(page = 0, size = 10, sort = emptyList()),
        )
    }

    private fun recommendationProjection(
        laptopId: Long? = 1L,
        price: Int? = 1_200_000,
    ): RecommendationCandidateProjection {
        return object : RecommendationCandidateProjection {
            override val laptopId: Long? = laptopId
            override val name: String = "Laptop"
            override val imageUrl: String = "https://example.com/laptop.jpg"
            override val price: Int? = price
            override val weight: Double? = 1.2
            override val screenSize: Int? = 14
            override val cpu: String? = "Core Ultra 7"
            override val graphicsType: String? = "Intel Graphics"
            override val resolution: String? = "2560x1600"
            override val portabilityScore: Int = 90
            override val displayScore: Int = 80
            override val ramScore: Int = 70
            override val tgpScore: Int = 25
            override val cpuPerformanceScore: Int = 75
            override val lowPowerCpuScore: Int = 85
            override val gpuPerformanceScore: Int = 40
            override val gpuCreatorBonus: Int = 0
            override val officeScore: Int = 95
            override val batteryScore: Int = 88
            override val casualGameScore: Int = 55
            override val onlineGameScore: Int = 45
            override val aaaGameScore: Int = 35
            override val creatorScore: Int = 60
        }
    }
}
