package going9.laptopgg.integration.recommendation.support

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.SaveCrawledLaptopUseCase
import going9.laptopgg.application.crawler.persistence.SaveResult
import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand
import going9.laptopgg.application.crawler.recommendation.port.RecommendationScorePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopProfile
import going9.laptopgg.recommendation.RecommendationGateInputs
import going9.laptopgg.recommendation.RecommendationScoreInputs
import going9.laptopgg.recommendation.RecommendationScoringPolicy
import going9.laptopgg.recommendation.RecommendationUseCase
import java.time.LocalDateTime

class RecommendationIntegrationFixtures(
    private val laptopRepository: CrawlerLaptopRepository,
    private val laptopProfileRepository: CrawlerLaptopProfileRepository,
    private val saveCrawledLaptopUseCase: SaveCrawledLaptopUseCase,
    private val recommendationScorePort: RecommendationScorePort,
    private val crawlerTransactionPort: CrawlerTransactionPort,
) {
    private val recommendationScoringPolicy = RecommendationScoringPolicy()

    fun persistSortProbeLaptops(): List<Laptop> {
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

    fun overrideProfileScores(
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

    fun overrideSortProbeScores(laptops: List<Laptop>) {
        require(laptops.size >= SORT_PROBE_COUNT) { "Sort probe score overrides require at least $SORT_PROBE_COUNT laptops." }

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
    }

    fun saveProfileAndScores(profile: LaptopProfile) {
        val savedProfile = laptopProfileRepository.save(profile)
        val laptopId = requireNotNull(savedProfile.laptop.id)
        crawlerTransactionPort.write {
            recommendationScorePort.saveAll(scoreCommands(laptopId, savedProfile))
        }
    }

    fun persistLaptop(
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
        val productCode = name.hashCode().toString()
        val result = saveCrawledLaptopUseCase.saveOrUpdateLaptop(
            CrawledLaptopCommand(
                name = name,
                imageUrl = "https://example.com/$productCode.jpg",
                detailPage = "https://example.com/$productCode",
                productCode = productCode,
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
                lastDetailedCrawledAt = null,
                usages = usages,
            ),
        )
        require(result == SaveResult.CREATED || result == SaveResult.UPDATED) {
            "Fixture laptop must be created or updated, but was $result."
        }

        return laptopRepository.findAllWithUsageByProductCodeIn(listOf(productCode)).singleOrNull()
            ?: error("Fixture laptop was saved but could not be loaded for productCode=$productCode.")
    }

    private companion object {
        const val SORT_PROBE_COUNT = 4
    }

    private fun scoreCommands(
        laptopId: Long,
        profile: LaptopProfile,
    ): List<UpsertRecommendationScoreCommand> {
        val scoreInputs = RecommendationScoreInputs(
            budgetScore = 0,
            portabilityScore = profile.portabilityScore,
            displayScore = profile.displayScore,
            ramScore = profile.ramScore,
            tgpScore = profile.tgpScore,
            cpuPerformanceScore = profile.cpuPerformanceScore,
            lowPowerCpuScore = profile.lowPowerCpuScore,
            gpuScore = profile.gpuPerformanceScore,
            creatorGpuScore = (profile.gpuPerformanceScore + profile.gpuCreatorBonus).coerceAtMost(100),
            officeScore = profile.officeScore,
            batteryScore = profile.batteryScore,
        )
        val gateInputs = RecommendationGateInputs(
            officeScore = profile.officeScore,
            batteryScore = profile.batteryScore,
            casualGameScore = profile.casualGameScore,
            onlineGameScore = profile.onlineGameScore,
            aaaGameScore = profile.aaaGameScore,
            creatorScore = profile.creatorScore,
        )
        val updatedAt = LocalDateTime.now()

        return RecommendationUseCase.entries.map { useCase ->
            UpsertRecommendationScoreCommand(
                laptopId = laptopId,
                useCase = useCase,
                gateScore = recommendationScoringPolicy.gateScore(gateInputs, useCase),
                staticScore = recommendationScoringPolicy.staticScore(useCase, scoreInputs),
                budgetWeight = recommendationScoringPolicy.budgetWeight(useCase),
                updatedAt = updatedAt,
            )
        }
    }
}
