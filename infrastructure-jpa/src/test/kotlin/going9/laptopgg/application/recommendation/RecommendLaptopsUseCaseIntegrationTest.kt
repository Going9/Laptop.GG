package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder
import going9.laptopgg.application.crawler.LaptopProfileService
import going9.laptopgg.application.crawler.RecommendationScoreService
import going9.laptopgg.application.service.ScoreCalculatorService
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopUsage
import going9.laptopgg.domain.laptop.BatteryTier
import going9.laptopgg.domain.laptop.CpuClass
import going9.laptopgg.domain.laptop.GpuClass
import going9.laptopgg.domain.laptop.PortabilityTier
import going9.laptopgg.infrastructure.jpa.repository.shared.LaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.shared.LaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.shared.LaptopUsageRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.profiles.active=test,crawler"])
@Transactional
class RecommendLaptopsUseCaseIntegrationTest {
    @Autowired
    lateinit var recommendLaptopsUseCase: RecommendLaptopsUseCase

    @Autowired
    lateinit var laptopRepository: LaptopRepository

    @Autowired
    lateinit var laptopUsageRepository: LaptopUsageRepository

    @Autowired
    lateinit var laptopProfileRepository: LaptopProfileRepository

    @Autowired
    lateinit var laptopProfileService: LaptopProfileService

    @Autowired
    lateinit var scoreCalculatorService: ScoreCalculatorService

    @Autowired
    lateinit var recommendationScoreService: RecommendationScoreService

    @Autowired
    lateinit var recommendationScoreRepository: RecommendationScoreRepository

    @BeforeEach
    fun setUp() {
        recommendationScoreRepository.deleteAll()
        laptopProfileRepository.deleteAll()
        laptopUsageRepository.deleteAll()
        laptopRepository.deleteAll()
    }

    @Test
    fun `recommendation paging keeps higher scored laptops on earlier pages`() {
        persistLaptop(
            name = "Office Feather",
            price = 1_100_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 80.0,
            weight = 1.12,
            usages = listOf("사무/인강용", "휴대용"),
        )
        persistLaptop(
            name = "Office Standard",
            price = 1_250_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 65.0,
            weight = 1.55,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
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
    fun `battery first includes high battery modern h and 300 series cpus`() {
        persistLaptop(
            name = "Battery 255H",
            price = 1_950_000,
            cpuManufacturer = "인텔",
            cpu = "255H",
            graphicsType = "Arc 140T",
            batteryCapacity = 86.0,
            weight = 1.45,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Battery 350",
            price = 1_850_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 80.0,
            weight = 1.38,
            usages = listOf("사무/인강용", "휴대용"),
        )
        persistLaptop(
            name = "Battery 340",
            price = 1_650_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 75.0,
            weight = 1.3,
            usages = listOf("사무/인강용", "휴대용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_500_000,
            maxWeightKg = 2.0,
            screenSizes = listOf(13, 14, 15, 16),
            useCase = RecommendationUseCase.BATTERY_FIRST,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))
        val names = result.content.map { it.name }

        assertThat(names).contains("Battery 255H", "Battery 350", "Battery 340")
    }

    @Test
    fun `casual game includes arc radeon and intel integrated graphics families`() {
        persistLaptop(
            name = "Arc Casual",
            price = 1_700_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 78.0,
            weight = 1.34,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Radeon Casual",
            price = 1_600_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 860M",
            batteryCapacity = 73.0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Intel Casual",
            price = 1_300_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 65.0,
            weight = 1.4,
            usages = listOf("사무/인강용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizes = listOf(13, 14, 15, 16),
            useCase = RecommendationUseCase.CASUAL_GAME,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))
        val names = result.content.map { it.name }

        assertThat(names).contains("Arc Casual", "Radeon Casual", "Intel Casual")
    }

    @Test
    fun `online and aaa gaming recognize latest rtx 50 series`() {
        persistLaptop(
            name = "RTX 5060 Online",
            price = 1_900_000,
            cpuManufacturer = "인텔",
            cpu = "255H",
            graphicsType = "RTX5060",
            batteryCapacity = 75.0,
            weight = 2.1,
            tgp = 100,
            ramSize = 16,
            usages = listOf("게임용"),
        )
        persistLaptop(
            name = "RTX 5070 Ti Online",
            price = 2_500_000,
            cpuManufacturer = "인텔",
            cpu = "275HX",
            graphicsType = "RTX5070 Ti",
            batteryCapacity = 90.0,
            weight = 2.5,
            tgp = 140,
            ramSize = 32,
            usages = listOf("게임용"),
        )
        persistLaptop(
            name = "RTX 5090 AAA",
            price = 4_800_000,
            cpuManufacturer = "인텔",
            cpu = "275HX",
            graphicsType = "RTX5090",
            batteryCapacity = 99.0,
            weight = 2.8,
            tgp = 175,
            ramSize = 32,
            usages = listOf("게임용"),
        )

        val onlineRequest = LaptopRecommendationQuery(
            budget = 5_000_000,
            maxWeightKg = 3.0,
            screenSizes = listOf(15, 16, 17, 18),
            useCase = RecommendationUseCase.ONLINE_GAME,
        )
        val aaaRequest = LaptopRecommendationQuery(
            budget = 5_000_000,
            maxWeightKg = 3.0,
            screenSizes = listOf(15, 16, 17, 18),
            useCase = RecommendationUseCase.AAA_GAME,
        )

        val onlineResult = recommendLaptopsUseCase.recommend(onlineRequest, page(0, 10))
        val aaaResult = recommendLaptopsUseCase.recommend(aaaRequest, page(0, 10))

        assertThat(onlineResult.content.map { it.name }).contains("RTX 5060 Online", "RTX 5070 Ti Online")
        assertThat(aaaResult.content.first().name).isEqualTo("RTX 5090 AAA")
    }

    @Test
    fun `screen size mode supports select any and not sure flows`() {
        persistLaptop(
            name = "Compact 14",
            price = 1_200_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 72.0,
            weight = 1.25,
            screenSize = 14,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Large 17",
            price = 1_300_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 74.0,
            weight = 1.8,
            screenSize = 17,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Unknown Screen",
            price = 1_250_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 76.0,
            weight = 1.4,
            screenSize = null,
            usages = listOf("사무/인강용"),
        )

        val selectRequest = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizes = listOf(14),
            screenSizeMode = ScreenSizeMode.SELECT,
            useCase = RecommendationUseCase.NOT_SURE,
        )
        val anyRequest = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizeMode = ScreenSizeMode.ANY,
            useCase = RecommendationUseCase.NOT_SURE,
        )
        val notSureRequest = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizeMode = ScreenSizeMode.NOT_SURE,
            useCase = RecommendationUseCase.NOT_SURE,
        )

        val selectResult = recommendLaptopsUseCase.recommend(selectRequest, page(0, 10))
        val anyResult = recommendLaptopsUseCase.recommend(anyRequest, page(0, 10))
        val notSureResult = recommendLaptopsUseCase.recommend(notSureRequest, page(0, 10))

        assertThat(selectResult.content.map { it.name }).containsExactly("Compact 14")
        assertThat(anyResult.content.map { it.name }).contains("Compact 14", "Large 17", "Unknown Screen")
        assertThat(notSureResult.content.map { it.name }).contains("Compact 14", "Unknown Screen")
        assertThat(notSureResult.content.map { it.name }).doesNotContain("Large 17")
    }

    @Test
    fun `recommendation list includes cpu gpu and friendly resolution label`() {
        persistLaptop(
            name = "Display Friendly",
            price = 1_550_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 78.0,
            weight = 1.34,
            usages = listOf("사무/인강용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizes = listOf(14, 15, 16),
            useCase = RecommendationUseCase.OFFICE_STUDY,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))
        val laptop = result.content.first { it.name == "Display Friendly" }

        assertThat(laptop.cpu).isEqualTo("350")
        assertThat(laptop.gpu).isEqualTo("Arc 140T")
        assertThat(laptop.resolutionLabel).isEqualTo("QHD")
    }

    @Test
    fun `office recommendation excludes profiles below gate threshold at query stage`() {
        val officeLaptop = persistLaptop(
            name = "Office Strong",
            price = 1_450_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 72.0,
            weight = 1.28,
            usages = listOf("사무/인강용"),
        )
        val weakOfficeLaptop = persistLaptop(
            name = "Office Weak",
            price = 1_350_000,
            cpuManufacturer = "인텔",
            cpu = "255H",
            graphicsType = "RTX4060",
            batteryCapacity = 48.0,
            weight = 2.45,
            usages = listOf("게임용"),
        )

        laptopProfileRepository.findByLaptopId(officeLaptop.id!!)?.apply {
            cpuClass = CpuClass.LOW_POWER
            gpuClass = GpuClass.INTEGRATED_MAINSTREAM
            batteryTier = BatteryTier.HIGH
            portabilityTier = PortabilityTier.LIGHT
            officeScore = 82
            batteryScore = 76
            casualGameScore = 40
            onlineGameScore = 22
            aaaGameScore = 10
            creatorScore = 38
        }?.let(::saveProfileAndScores)

        laptopProfileRepository.findByLaptopId(weakOfficeLaptop.id!!)?.apply {
            cpuClass = CpuClass.PERFORMANCE
            gpuClass = GpuClass.DISCRETE_HIGH
            batteryTier = BatteryTier.LOW
            portabilityTier = PortabilityTier.HEAVY
            officeScore = 35
            batteryScore = 30
            casualGameScore = 78
            onlineGameScore = 84
            aaaGameScore = 72
            creatorScore = 66
        }?.let(::saveProfileAndScores)

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 3.0,
            screenSizeMode = ScreenSizeMode.ANY,
            useCase = RecommendationUseCase.OFFICE_STUDY,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))

        assertThat(result.content.map { it.name }).contains("Office Strong")
        assertThat(result.content.map { it.name }).doesNotContain("Office Weak")
    }

    @Test
    fun `not sure recommendation keeps rounded average boundary candidate`() {
        val borderlineLaptop = persistLaptop(
            name = "Not Sure Borderline",
            price = 1_380_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 70.0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )
        val filteredLaptop = persistLaptop(
            name = "Not Sure Below Boundary",
            price = 1_340_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 66.0,
            weight = 1.4,
            usages = listOf("사무/인강용"),
        )

        laptopProfileRepository.findByLaptopId(borderlineLaptop.id!!)?.apply {
            officeScore = 46
            batteryScore = 44
            casualGameScore = 44
        }?.let(::saveProfileAndScores)

        laptopProfileRepository.findByLaptopId(filteredLaptop.id!!)?.apply {
            officeScore = 45
            batteryScore = 44
            casualGameScore = 44
        }?.let(::saveProfileAndScores)

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 2.0,
            screenSizeMode = ScreenSizeMode.ANY,
            useCase = RecommendationUseCase.NOT_SURE,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))

        assertThat(result.content.map { it.name }).contains("Not Sure Borderline")
        assertThat(result.content.map { it.name }).doesNotContain("Not Sure Below Boundary")
    }

    @Test
    fun `unknown weight laptops are not excluded from recommendation candidates`() {
        persistLaptop(
            name = "Known Weight",
            price = 1_300_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 74.0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Unknown Weight",
            price = 1_250_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 76.0,
            weight = null,
            usages = listOf("사무/인강용"),
        )

        val request = LaptopRecommendationQuery(
            budget = 2_000_000,
            maxWeightKg = 1.4,
            screenSizes = listOf(13, 14, 15, 16),
            useCase = RecommendationUseCase.NOT_SURE,
        )

        val result = recommendLaptopsUseCase.recommend(request, page(0, 10))

        assertThat(result.content.map { it.name }).contains("Known Weight", "Unknown Weight")
    }

    @Test
    fun `weight descending keeps unknown weight at the end`() {
        persistLaptop(
            name = "Weight 1.8",
            price = 1_350_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 70.0,
            weight = 1.8,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
            name = "Weight 1.3",
            price = 1_250_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 76.0,
            weight = 1.3,
            usages = listOf("사무/인강용"),
        )
        persistLaptop(
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
            page(0, 10, sortOrder("weight", SortDirection.DESC)),
        )

        assertThat(result.content.map { it.name }).endsWith("Weight Unknown")
    }

    @Test
    fun `recommended database pages match calculator order for every use case`() {
        val laptops = persistSortProbeLaptops()
        overrideProfileScores(
            laptop = laptops[0],
            officeScore = 95,
            batteryScore = 85,
            casualGameScore = 75,
            onlineGameScore = 75,
            aaaGameScore = 75,
            creatorScore = 80,
            cpuPerformanceScore = 75,
            lowPowerCpuScore = 85,
            gpuPerformanceScore = 70,
            gpuCreatorBonus = 5,
            portabilityScore = 95,
            displayScore = 83,
            ramScore = 75,
            tgpScore = 70,
        )
        overrideProfileScores(
            laptop = laptops[1],
            officeScore = 75,
            batteryScore = 70,
            casualGameScore = 95,
            onlineGameScore = 98,
            aaaGameScore = 98,
            creatorScore = 90,
            cpuPerformanceScore = 90,
            lowPowerCpuScore = 55,
            gpuPerformanceScore = 98,
            gpuCreatorBonus = 8,
            portabilityScore = 50,
            displayScore = 90,
            ramScore = 90,
            tgpScore = 98,
        )
        overrideProfileScores(
            laptop = laptops[2],
            officeScore = 85,
            batteryScore = 80,
            casualGameScore = 82,
            onlineGameScore = 84,
            aaaGameScore = 80,
            creatorScore = 98,
            cpuPerformanceScore = 95,
            lowPowerCpuScore = 75,
            gpuPerformanceScore = 90,
            gpuCreatorBonus = 10,
            portabilityScore = 82,
            displayScore = 98,
            ramScore = 100,
            tgpScore = 80,
        )
        overrideProfileScores(
            laptop = laptops[3],
            officeScore = 88,
            batteryScore = 90,
            casualGameScore = 72,
            onlineGameScore = 72,
            aaaGameScore = 70,
            creatorScore = 75,
            cpuPerformanceScore = 70,
            lowPowerCpuScore = 92,
            gpuPerformanceScore = 65,
            gpuCreatorBonus = 0,
            portabilityScore = 100,
            displayScore = 75,
            ramScore = 70,
            tgpScore = 65,
        )

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
                    val profile = laptopProfileRepository.findByLaptopId(response.id)!!
                    CalculatorSortProbe(
                        name = response.name,
                        score = scoreCalculatorService.calculateScore(profile.laptop, profile, request).score,
                        price = profile.laptop.price,
                        id = profile.laptop.id!!,
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
        persistSortProbeLaptops().forEach { laptop ->
            overrideProfileScores(
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

        assertThat(pagedNames(request, sortOrder("price", SortDirection.ASC)))
            .isEqualTo(listOf("Budget Light", "Balanced Value", "Creator Slim", "Gaming Power"))
        assertThat(pagedNames(request, sortOrder("price", SortDirection.DESC)))
            .isEqualTo(listOf("Gaming Power", "Creator Slim", "Balanced Value", "Budget Light"))
        assertThat(pagedNames(request, sortOrder("weight", SortDirection.ASC)))
            .isEqualTo(listOf("Budget Light", "Balanced Value", "Creator Slim", "Gaming Power"))
        assertThat(pagedNames(request, sortOrder("weight", SortDirection.DESC)))
            .isEqualTo(listOf("Gaming Power", "Creator Slim", "Balanced Value", "Budget Light"))
    }

    private fun persistSortProbeLaptops(): List<Laptop> {
        return listOf(
            persistLaptop(
                name = "Balanced Value",
                price = 1_000_000,
                cpuManufacturer = "인텔",
                cpu = "225U",
                graphicsType = "Intel Graphics",
                batteryCapacity = 70.0,
                weight = 1.2,
                usages = listOf("사무/인강용"),
            ),
            persistLaptop(
                name = "Gaming Power",
                price = 1_600_000,
                cpuManufacturer = "인텔",
                cpu = "275HX",
                graphicsType = "RTX5070",
                batteryCapacity = 82.0,
                weight = 2.0,
                tgp = 140,
                usages = listOf("게임용"),
            ),
            persistLaptop(
                name = "Creator Slim",
                price = 1_400_000,
                cpuManufacturer = "AMD",
                cpu = "370",
                graphicsType = "Radeon 890M",
                batteryCapacity = 78.0,
                weight = 1.4,
                usages = listOf("그래픽작업용"),
            ),
            persistLaptop(
                name = "Budget Light",
                price = 800_000,
                cpuManufacturer = "AMD",
                cpu = "340",
                graphicsType = "Radeon 840M",
                batteryCapacity = 76.0,
                weight = 1.1,
                usages = listOf("휴대용"),
            ),
        )
    }

    private fun overrideProfileScores(
        laptop: Laptop,
        officeScore: Int,
        batteryScore: Int,
        casualGameScore: Int,
        onlineGameScore: Int,
        aaaGameScore: Int,
        creatorScore: Int,
        cpuPerformanceScore: Int,
        lowPowerCpuScore: Int,
        gpuPerformanceScore: Int,
        gpuCreatorBonus: Int,
        portabilityScore: Int,
        displayScore: Int,
        ramScore: Int,
        tgpScore: Int,
    ) {
        laptopProfileRepository.findByLaptopId(laptop.id!!)?.apply {
            this.officeScore = officeScore
            this.batteryScore = batteryScore
            this.casualGameScore = casualGameScore
            this.onlineGameScore = onlineGameScore
            this.aaaGameScore = aaaGameScore
            this.creatorScore = creatorScore
            this.cpuPerformanceScore = cpuPerformanceScore
            this.lowPowerCpuScore = lowPowerCpuScore
            this.gpuPerformanceScore = gpuPerformanceScore
            this.gpuCreatorBonus = gpuCreatorBonus
            this.portabilityScore = portabilityScore
            this.displayScore = displayScore
            this.ramScore = ramScore
            this.tgpScore = tgpScore
        }?.let(::saveProfileAndScores)
    }

    private fun saveProfileAndScores(profile: going9.laptopgg.domain.laptop.LaptopProfile) {
        val savedProfile = laptopProfileRepository.save(profile)
        recommendationScoreService.refreshScores(savedProfile)
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

    private fun page(page: Int, size: Int, vararg orders: SortOrder): PageQuery {
        return PageQuery(
            page = page,
            size = size,
            sort = orders.toList(),
        )
    }

    private fun sortOrder(property: String, direction: SortDirection): SortOrder {
        return SortOrder(
            property = property,
            direction = direction,
        )
    }

    private data class CalculatorSortProbe(
        val name: String,
        val score: Double,
        val price: Int?,
        val id: Long,
    )

    private fun persistLaptop(
        name: String,
        price: Int,
        cpuManufacturer: String,
        cpu: String,
        graphicsType: String,
        batteryCapacity: Double,
        weight: Double?,
        screenSize: Int? = 16,
        tgp: Int = 0,
        ramSize: Int = 16,
        usages: List<String>,
    ): Laptop {
        val laptop = Laptop(
            name = name,
            imageUrl = "https://example.com/${name.hashCode()}.jpg",
            detailPage = "https://example.com/${name.hashCode()}",
            productCode = name.hashCode().toString(),
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = "윈도우11홈",
            screenSize = screenSize,
            resolution = "2560x1600(WQXGA)",
            brightness = 400,
            refreshRate = 165,
            ramSize = ramSize,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = graphicsType,
            tgp = tgp,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 2,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = batteryCapacity,
            storageCapacity = 1024,
            storageSlotCount = 1,
            weight = weight,
            laptopUsage = mutableListOf(),
        )

        laptop.laptopUsage = usages
            .map { usage -> LaptopUsage(usage = usage, laptop = laptop) }
            .toMutableList()

        val savedLaptop = laptopRepository.save(laptop)
        laptopProfileService.syncProfile(savedLaptop)
        return savedLaptop
    }
}
