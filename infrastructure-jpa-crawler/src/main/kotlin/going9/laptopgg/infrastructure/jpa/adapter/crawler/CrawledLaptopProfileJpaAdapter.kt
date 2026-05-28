package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.application.crawler.profile.UpsertCrawledLaptopProfileCommand
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.persistence.model.laptop.LaptopProfile
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CrawledLaptopProfileJpaAdapter(
    private val laptopProfileRepository: CrawlerLaptopProfileRepository,
    private val laptopRepository: CrawlerLaptopRepository,
) : CrawledLaptopProfilePort {
    override fun upsert(command: UpsertCrawledLaptopProfileCommand): CrawledLaptopProfileState {
        val existingProfile = laptopProfileRepository.findByLaptopId(command.laptopId)
        if (existingProfile != null) {
            return if (existingProfile.applySnapshot(command.profile)) {
                laptopProfileRepository.save(existingProfile).toState()
            } else {
                existingProfile.toState()
            }
        }

        return laptopProfileRepository.save(newProfile(command)).toState()
    }

    override fun findLaptopIdsWithIncompleteStaticScores(limit: Int): List<Long> {
        if (limit <= 0) {
            return emptyList()
        }
        return laptopProfileRepository.findLaptopIdsWithIncompleteStaticScores(PageRequest.of(0, limit))
    }

    private fun newProfile(command: UpsertCrawledLaptopProfileCommand): LaptopProfile {
        val snapshot = command.profile
        return LaptopProfile(
            laptop = laptopRepository.getReferenceById(command.laptopId),
            cpuClass = snapshot.cpuClass,
            gpuClass = snapshot.gpuClass,
            batteryTier = snapshot.batteryTier,
            portabilityTier = snapshot.portabilityTier,
            officeScore = snapshot.officeScore,
            batteryScore = snapshot.batteryScore,
            casualGameScore = snapshot.casualGameScore,
            onlineGameScore = snapshot.onlineGameScore,
            aaaGameScore = snapshot.aaaGameScore,
            creatorScore = snapshot.creatorScore,
            cpuPerformanceScore = snapshot.cpuPerformanceScore,
            lowPowerCpuScore = snapshot.lowPowerCpuScore,
            gpuPerformanceScore = snapshot.gpuPerformanceScore,
            gpuCreatorBonus = snapshot.gpuCreatorBonus,
            portabilityScore = snapshot.portabilityScore,
            displayScore = snapshot.displayScore,
            ramScore = snapshot.ramScore,
            tgpScore = snapshot.tgpScore,
        )
    }

    private fun LaptopProfile.applySnapshot(snapshot: LaptopProfileSnapshot): Boolean {
        var changed = false
        changed = updateField(cpuClass, snapshot.cpuClass) { cpuClass = it } || changed
        changed = updateField(gpuClass, snapshot.gpuClass) { gpuClass = it } || changed
        changed = updateField(batteryTier, snapshot.batteryTier) { batteryTier = it } || changed
        changed = updateField(portabilityTier, snapshot.portabilityTier) { portabilityTier = it } || changed
        changed = updateField(officeScore, snapshot.officeScore) { officeScore = it } || changed
        changed = updateField(batteryScore, snapshot.batteryScore) { batteryScore = it } || changed
        changed = updateField(casualGameScore, snapshot.casualGameScore) { casualGameScore = it } || changed
        changed = updateField(onlineGameScore, snapshot.onlineGameScore) { onlineGameScore = it } || changed
        changed = updateField(aaaGameScore, snapshot.aaaGameScore) { aaaGameScore = it } || changed
        changed = updateField(creatorScore, snapshot.creatorScore) { creatorScore = it } || changed
        changed = updateField(cpuPerformanceScore, snapshot.cpuPerformanceScore) { cpuPerformanceScore = it } || changed
        changed = updateField(lowPowerCpuScore, snapshot.lowPowerCpuScore) { lowPowerCpuScore = it } || changed
        changed = updateField(gpuPerformanceScore, snapshot.gpuPerformanceScore) { gpuPerformanceScore = it } || changed
        changed = updateField(gpuCreatorBonus, snapshot.gpuCreatorBonus) { gpuCreatorBonus = it } || changed
        changed = updateField(portabilityScore, snapshot.portabilityScore) { portabilityScore = it } || changed
        changed = updateField(displayScore, snapshot.displayScore) { displayScore = it } || changed
        changed = updateField(ramScore, snapshot.ramScore) { ramScore = it } || changed
        changed = updateField(tgpScore, snapshot.tgpScore) { tgpScore = it } || changed
        return changed
    }

    private fun LaptopProfile.toState(): CrawledLaptopProfileState {
        return CrawledLaptopProfileState(
            laptopId = requireNotNull(laptop.id) { "Laptop profile must reference a persisted laptop." },
            profile = LaptopProfileSnapshot(
                cpuClass = cpuClass,
                gpuClass = gpuClass,
                batteryTier = batteryTier,
                portabilityTier = portabilityTier,
                officeScore = officeScore,
                batteryScore = batteryScore,
                casualGameScore = casualGameScore,
                onlineGameScore = onlineGameScore,
                aaaGameScore = aaaGameScore,
                creatorScore = creatorScore,
                cpuPerformanceScore = cpuPerformanceScore,
                lowPowerCpuScore = lowPowerCpuScore,
                gpuPerformanceScore = gpuPerformanceScore,
                gpuCreatorBonus = gpuCreatorBonus,
                portabilityScore = portabilityScore,
                displayScore = displayScore,
                ramScore = ramScore,
                tgpScore = tgpScore,
            ),
        )
    }

    private fun <T> updateField(currentValue: T, newValue: T, updater: (T) -> Unit): Boolean {
        if (currentValue == newValue) {
            return false
        }

        updater(newValue)
        return true
    }
}
