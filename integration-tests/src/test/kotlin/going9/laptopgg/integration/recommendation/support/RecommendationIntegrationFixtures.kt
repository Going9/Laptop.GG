package going9.laptopgg.integration.recommendation.support

import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.application.crawler.profile.LaptopProfileService
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.recommendation.RecommendationScoreService
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopProfile
import going9.laptopgg.persistence.model.laptop.LaptopUsage

class RecommendationIntegrationFixtures(
    private val laptopRepository: CrawlerLaptopRepository,
    private val laptopProfileRepository: CrawlerLaptopProfileRepository,
    private val laptopProfileService: LaptopProfileService,
    private val recommendationScoreService: RecommendationScoreService,
) {
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

    fun saveProfileAndScores(profile: LaptopProfile) {
        val savedProfile = laptopProfileRepository.save(profile)
        recommendationScoreService.refreshScores(
            CrawledLaptopProfileState(
                laptopId = requireNotNull(savedProfile.laptop.id),
                profile = LaptopProfileSnapshot(
                    cpuClass = savedProfile.cpuClass,
                    gpuClass = savedProfile.gpuClass,
                    batteryTier = savedProfile.batteryTier,
                    portabilityTier = savedProfile.portabilityTier,
                    officeScore = savedProfile.officeScore,
                    batteryScore = savedProfile.batteryScore,
                    casualGameScore = savedProfile.casualGameScore,
                    onlineGameScore = savedProfile.onlineGameScore,
                    aaaGameScore = savedProfile.aaaGameScore,
                    creatorScore = savedProfile.creatorScore,
                    cpuPerformanceScore = savedProfile.cpuPerformanceScore,
                    lowPowerCpuScore = savedProfile.lowPowerCpuScore,
                    gpuPerformanceScore = savedProfile.gpuPerformanceScore,
                    gpuCreatorBonus = savedProfile.gpuCreatorBonus,
                    portabilityScore = savedProfile.portabilityScore,
                    displayScore = savedProfile.displayScore,
                    ramScore = savedProfile.ramScore,
                    tgpScore = savedProfile.tgpScore,
                ),
            ),
        )
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
        laptopProfileService.syncProfile(savedLaptop.toCrawledSnapshot())
        return savedLaptop
    }

    private fun Laptop.toCrawledSnapshot(): PersistedCrawledLaptopSnapshot {
        return PersistedCrawledLaptopSnapshot(
            id = requireNotNull(id),
            name = name,
            imageUrl = imageUrl,
            detailPage = detailPage,
            productCode = productCode,
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = os,
            screenSize = screenSize,
            resolution = resolution,
            brightness = brightness,
            refreshRate = refreshRate,
            ramSize = ramSize,
            ramType = ramType,
            isRamReplaceable = isRamReplaceable,
            graphicsType = graphicsType,
            tgp = tgp,
            thunderboltCount = thunderboltCount,
            usbCCount = usbCCount,
            usbACount = usbACount,
            sdCard = sdCard,
            isSupportsPdCharging = isSupportsPdCharging,
            batteryCapacity = batteryCapacity,
            storageCapacity = storageCapacity,
            storageSlotCount = storageSlotCount,
            weight = weight,
            lastDetailedCrawledAt = lastDetailedCrawledAt,
            usages = laptopUsage.map { usage -> usage.usage },
        )
    }
}
