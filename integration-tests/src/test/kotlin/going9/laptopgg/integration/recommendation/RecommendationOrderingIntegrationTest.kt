package going9.laptopgg.integration.recommendation

import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder
import going9.laptopgg.application.common.SortProperty
import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.integration.recommendation.support.RecommendationIntegrationTestSupport
import going9.laptopgg.recommendation.RecommendationUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationOrderingIntegrationTest : RecommendationIntegrationTestSupport() {
    @Test
    fun `recommendation paging keeps higher scored laptops on earlier pages`() {
        fixtures.persistLaptop(
            name = "Office Feather",
            price = 1_100_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 80.0,
            weight = 1.12,
            usages = listOf("사무/인강용", "휴대용"),
        )
        fixtures.persistLaptop(
            name = "Office Standard",
            price = 1_250_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 65.0,
            weight = 1.55,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
            name = "Office Heavy",
            price = 1_300_000,
            cpuManufacturer = "AMD",
            cpu = "5500U",
            graphicsType = "Radeon Graphics",
            batteryCapacity = 55.0,
            weight = 1.95,
            usages = listOf("사무/인강용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.2,
            screenSizes = listOf(13, 14, 15, 16),
            useCase = RecommendationUseCase.OFFICE_STUDY,
        )

        val firstPage = recommendLaptopsUseCase.recommend(request, page(0, 1))
        val secondPage = recommendLaptopsUseCase.recommend(request, page(1, 1))

        assertThat(firstPage.content.first().score).isGreaterThanOrEqualTo(secondPage.content.first().score)
    }

    @Test
    fun `weight descending keeps unknown weight at the end`() {
        fixtures.persistLaptop(
            name = "Weight 1.8",
            price = 1_350_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 70.0,
            weight = 1.8,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
            name = "Weight 1.3",
            price = 1_250_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 76.0,
            weight = 1.3,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
            name = "Weight Unknown",
            price = 1_150_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 82.0,
            weight = null,
            usages = listOf("사무/인강용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizeMode = ScreenSizeMode.ANY,
            useCase = RecommendationUseCase.NOT_SURE,
        )

        val result = recommendLaptopsUseCase.recommend(
            request,
            page(0, 10, sortOrder(SortProperty.WEIGHT, SortDirection.DESC)),
        )

        assertThat(result.content.map { it.name }).endsWith("Weight Unknown")
    }

    @Test
    fun `recommended database pages match calculator order for every use case`() {
        val laptops = fixtures.persistSortProbeLaptops()
        fixtures.overrideSortProbeScores(laptops)

        RecommendationUseCase.entries.forEach { useCase ->
            val request = LaptopRecommendationQuery(
                budget = 2_000_000,
                maxWeightKg = 3.0,
                screenSizeMode = ScreenSizeMode.ANY,
                useCase = useCase,
            )
            val actual = listOf(
                recommendLaptopsUseCase.recommend(request, page(0, 2)).content,
                recommendLaptopsUseCase.recommend(request, page(1, 2)).content,
            ).flatten()

            val expectedNames = actual
                .map { response ->
                    CalculatorSortProbe(
                        name = response.name,
                        score = response.score,
                        price = response.price,
                        id = response.id,
                    )
                }
                .sortedWith(
                    compareByDescending<CalculatorSortProbe> { it.score }
                        .thenBy { it.price ?: Int.MAX_VALUE }
                        .thenBy { it.id },
                )
                .map { it.name }

            assertThat(actual.map { it.name })
                .describedAs("recommended order for $useCase")
                .isEqualTo(expectedNames)
        }
    }

    @Test
    fun `price and weight database pages keep requested order`() {
        fixtures.persistSortProbeLaptops().forEach { laptop ->
            fixtures.overrideProfileScores(
                laptop = laptop,
                officeScore = 85,
                batteryScore = 85,
                casualGameScore = 85,
                onlineGameScore = 85,
                aaaGameScore = 85,
                creatorScore = 85,
                cpuPerformanceScore = 85,
                lowPowerCpuScore = 85,
                gpuPerformanceScore = 85,
                gpuCreatorBonus = 0,
                portabilityScore = 85,
                displayScore = 85,
                ramScore = 85,
                tgpScore = 85,
            )
        }

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 3.0,
            screenSizeMode = ScreenSizeMode.ANY,
            useCase = RecommendationUseCase.NOT_SURE,
        )

        assertThat(pagedNames(request, sortOrder(SortProperty.PRICE, SortDirection.ASC)))
            .isEqualTo(listOf("Budget Light", "Balanced Value", "Creator Slim", "Gaming Power"))
        assertThat(pagedNames(request, sortOrder(SortProperty.PRICE, SortDirection.DESC)))
            .isEqualTo(listOf("Gaming Power", "Creator Slim", "Balanced Value", "Budget Light"))
        assertThat(pagedNames(request, sortOrder(SortProperty.WEIGHT, SortDirection.ASC)))
            .isEqualTo(listOf("Budget Light", "Balanced Value", "Creator Slim", "Gaming Power"))
        assertThat(pagedNames(request, sortOrder(SortProperty.WEIGHT, SortDirection.DESC)))
            .isEqualTo(listOf("Gaming Power", "Creator Slim", "Balanced Value", "Budget Light"))
    }

    private fun pagedNames(
        request: LaptopRecommendationQuery,
        order: SortOrder,
    ): List<String> {
        return listOf(
            recommendLaptopsUseCase.recommend(request, page(0, 2, order)).content,
            recommendLaptopsUseCase.recommend(request, page(1, 2, order)).content,
        ).flatten().map { it.name }
    }

    private data class CalculatorSortProbe(
        val name: String,
        val score: Double,
        val price: Int?,
        val id: Long,
    )
}
