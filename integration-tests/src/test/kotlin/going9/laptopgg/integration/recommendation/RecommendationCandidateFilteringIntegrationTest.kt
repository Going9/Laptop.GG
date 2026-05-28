package going9.laptopgg.integration.recommendation

import going9.laptopgg.application.recommendation.LaptopRecommendationQuery
import going9.laptopgg.application.recommendation.ScreenSizeMode
import going9.laptopgg.integration.recommendation.support.RecommendationIntegrationTestSupport
import going9.laptopgg.recommendation.RecommendationUseCase
import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.CpuClass
import going9.laptopgg.taxonomy.GpuClass
import going9.laptopgg.taxonomy.PortabilityTier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RecommendationCandidateFilteringIntegrationTest : RecommendationIntegrationTestSupport() {
    @Test
    fun `battery first includes high battery modern h and 300 series cpus`() {
        fixtures.persistLaptop(
            name = "Battery 255H",
            price = 1_950_000,
            cpuManufacturer = "인텔",
            cpu = "255H",
            graphicsType = "Arc 140T",
            batteryCapacity = 86.0,
            weight = 1.45,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
            name = "Battery 350",
            price = 1_850_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 80.0,
            weight = 1.38,
            usages = listOf("사무/인강용", "휴대용"),
        )
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
            name = "Arc Casual",
            price = 1_700_000,
            cpuManufacturer = "인텔",
            cpu = "350",
            graphicsType = "Arc 140T",
            batteryCapacity = 78.0,
            weight = 1.34,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
            name = "Radeon Casual",
            price = 1_600_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 860M",
            batteryCapacity = 73.0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
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
        fixtures.persistLaptop(
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
    fun `office recommendation excludes profiles below gate threshold at query stage`() {
        val officeLaptop = fixtures.persistLaptop(
            name = "Office Strong",
            price = 1_450_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 72.0,
            weight = 1.28,
            usages = listOf("사무/인강용"),
        )
        val weakOfficeLaptop = fixtures.persistLaptop(
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
        }?.let(fixtures::saveProfileAndScores)

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
        }?.let(fixtures::saveProfileAndScores)

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
        val borderlineLaptop = fixtures.persistLaptop(
            name = "Not Sure Borderline",
            price = 1_380_000,
            cpuManufacturer = "AMD",
            cpu = "340",
            graphicsType = "Radeon 840M",
            batteryCapacity = 70.0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )
        val filteredLaptop = fixtures.persistLaptop(
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
        }?.let(fixtures::saveProfileAndScores)

        laptopProfileRepository.findByLaptopId(filteredLaptop.id!!)?.apply {
            officeScore = 45
            batteryScore = 44
            casualGameScore = 44
        }?.let(fixtures::saveProfileAndScores)

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
        fixtures.persistLaptop(
            name = "Known Weight",
            price = 1_300_000,
            cpuManufacturer = "인텔",
            cpu = "225U",
            graphicsType = "Intel Graphics",
            batteryCapacity = 74.0,
            weight = 1.35,
            usages = listOf("사무/인강용"),
        )
        fixtures.persistLaptop(
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
}
