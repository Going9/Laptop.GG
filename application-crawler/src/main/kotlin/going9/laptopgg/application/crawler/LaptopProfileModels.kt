package going9.laptopgg.application.crawler

import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.CpuClass
import going9.laptopgg.taxonomy.GpuClass
import going9.laptopgg.taxonomy.PortabilityTier

data class LaptopProfileSnapshot(
    val cpuClass: CpuClass,
    val gpuClass: GpuClass,
    val batteryTier: BatteryTier,
    val portabilityTier: PortabilityTier,
    val officeScore: Int,
    val batteryScore: Int,
    val casualGameScore: Int,
    val onlineGameScore: Int,
    val aaaGameScore: Int,
    val creatorScore: Int,
    val cpuPerformanceScore: Int,
    val lowPowerCpuScore: Int,
    val gpuPerformanceScore: Int,
    val gpuCreatorBonus: Int,
    val portabilityScore: Int,
    val displayScore: Int,
    val ramScore: Int,
    val tgpScore: Int,
)

data class UpsertCrawledLaptopProfileCommand(
    val laptopId: Long,
    val profile: LaptopProfileSnapshot,
)

data class CrawledLaptopProfileState(
    val laptopId: Long,
    val profile: LaptopProfileSnapshot,
)
