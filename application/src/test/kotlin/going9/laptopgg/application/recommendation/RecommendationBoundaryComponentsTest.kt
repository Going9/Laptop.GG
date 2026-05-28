package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder
import going9.laptopgg.application.common.SortProperty
import going9.laptopgg.application.recommendation.port.RecommendationCandidateRecord
import going9.laptopgg.application.recommendation.port.RecommendationCandidateSortMode
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationBoundaryComponentsTest {
    private val filterFactory = RecommendationCandidateFilterFactory()
    private val sortModeResolver = RecommendationSortModeResolver()
    private val resultMapper = LaptopRecommendationResultMapper()
    private val reasonBuilder = RecommendationReasonBuilder()

    @Test
    fun `candidate filter keeps selected screen policy and use case gate`() {
        val query = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 1.4,
            screenSizes = listOf(16, 14, 14),
            screenSizeMode = ScreenSizeMode.SELECT,
            useCase = RecommendationUseCase.OFFICE_STUDY,
        )

        val filter = filterFactory.create(query, RecommendationUseCase.OFFICE_STUDY, gateThreshold = 70)

        assertThat(filter.maxPrice).isEqualTo(2_000_000)
        assertThat(filter.maxWeight).isEqualTo(1.4)
        assertThat(filter.screenFilterEnabled).isTrue()
        assertThat(filter.includeUnknownScreen).isFalse()
        assertThat(filter.screenSizes).containsExactly(14, 16)
        assertThat(filter.minOfficeScore).isEqualTo(70)
    }

    @Test
    fun `not sure filter converts average gate threshold to total score`() {
        val query = LaptopRecommendationQuery(
            screenSizeMode = ScreenSizeMode.NOT_SURE,
            useCase = RecommendationUseCase.NOT_SURE,
        )

        val filter = filterFactory.create(query, RecommendationUseCase.NOT_SURE, gateThreshold = 70)

        assertThat(filter.screenFilterEnabled).isTrue()
        assertThat(filter.includeUnknownScreen).isTrue()
        assertThat(filter.screenSizes).containsExactly(13, 14, 15, 16)
        assertThat(filter.minNotSureGateTotal).isEqualTo(209)
    }

    @Test
    fun `sort mode resolver maps public sort orders to candidate query modes`() {
        assertThat(sortModeResolver.resolve(PageQuery(page = 0, size = 20)))
            .isEqualTo(RecommendationCandidateSortMode.RECOMMENDED)
        assertThat(
            sortModeResolver.resolve(
                PageQuery(
                    page = 0,
                    size = 20,
                    sort = listOf(SortOrder(SortProperty.PRICE, SortDirection.ASC)),
                ),
            ),
        ).isEqualTo(RecommendationCandidateSortMode.PRICE_ASC)
        assertThat(
            sortModeResolver.resolve(
                PageQuery(
                    page = 0,
                    size = 20,
                    sort = listOf(SortOrder(SortProperty.WEIGHT, SortDirection.DESC)),
                ),
            ),
        ).isEqualTo(RecommendationCandidateSortMode.WEIGHT_DESC)
    }

    @Test
    fun `result mapper keeps score output and derives display labels`() {
        val result = resultMapper.toResult(
            candidate = recommendationCandidate(
                name = "LG 그램 테스트",
                resolution = "2560x1600",
            ),
            scoreResult = RecommendationScoreCalculator.ScoreResult(
                score = 86.4,
                reasons = listOf("들고 다니기 편한 편이에요", "화면이 보기 편해요"),
            ),
        )

        assertThat(result.manufacturer).isEqualTo("LG")
        assertThat(result.resolutionLabel).isEqualTo("QHD")
        assertThat(result.score).isEqualTo(86.4)
        assertThat(result.reasons).containsExactly("들고 다니기 편한 편이에요", "화면이 보기 편해요")
    }

    @Test
    fun `reason builder returns strongest messages for use case`() {
        val reasons = reasonBuilder.build(
            useCase = RecommendationUseCase.ONLINE_GAME,
            candidate = recommendationCandidate(
                name = "게이밍 테스트",
                resolution = "1920x1080",
                gpuPerformanceScore = 93,
                onlineGameScore = 97,
                ramScore = 80,
            ),
            budgetScore = 60,
        )

        assertThat(reasons).containsExactly("온라인 게임에 잘 맞아요", "그래픽이 좋은 편이에요")
    }

    private fun recommendationCandidate(
        name: String,
        resolution: String?,
        gpuPerformanceScore: Int = 60,
        onlineGameScore: Int = 40,
        ramScore: Int = 80,
    ): RecommendationCandidateRecord {
        return RecommendationCandidateRecord(
            id = 1L,
            name = name,
            imageUrl = "https://img.example.com/laptop.jpg",
            price = 1_500_000,
            weight = 1.2,
            screenSize = 16,
            cpu = "Core Ultra 7",
            graphicsType = "Arc",
            resolution = resolution,
            portabilityScore = 80,
            displayScore = 90,
            ramScore = ramScore,
            tgpScore = 0,
            cpuPerformanceScore = 85,
            lowPowerCpuScore = 75,
            gpuPerformanceScore = gpuPerformanceScore,
            gpuCreatorBonus = 5,
            officeScore = 85,
            batteryScore = 80,
            casualGameScore = 60,
            onlineGameScore = onlineGameScore,
            aaaGameScore = 20,
            creatorScore = 70,
        )
    }
}
