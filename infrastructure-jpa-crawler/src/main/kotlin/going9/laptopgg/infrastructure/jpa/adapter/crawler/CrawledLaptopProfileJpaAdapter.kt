package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.UpsertCrawledLaptopProfileCommand
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
internal class CrawledLaptopProfileJpaAdapter(
    private val laptopProfileRepository: CrawlerLaptopProfileRepository,
    private val entityManager: EntityManager,
) : CrawledLaptopProfilePort {
    override fun upsert(command: UpsertCrawledLaptopProfileCommand): CrawledLaptopProfileState {
        val profile = command.profile
        val updatedRows = laptopProfileRepository.updateByLaptopId(
            laptopId = command.laptopId,
            cpuClass = profile.cpuClass,
            gpuClass = profile.gpuClass,
            batteryTier = profile.batteryTier,
            portabilityTier = profile.portabilityTier,
            officeScore = profile.officeScore,
            batteryScore = profile.batteryScore,
            casualGameScore = profile.casualGameScore,
            onlineGameScore = profile.onlineGameScore,
            aaaGameScore = profile.aaaGameScore,
            creatorScore = profile.creatorScore,
            cpuPerformanceScore = profile.cpuPerformanceScore,
            lowPowerCpuScore = profile.lowPowerCpuScore,
            gpuPerformanceScore = profile.gpuPerformanceScore,
            gpuCreatorBonus = profile.gpuCreatorBonus,
            portabilityScore = profile.portabilityScore,
            displayScore = profile.displayScore,
            ramScore = profile.ramScore,
            tgpScore = profile.tgpScore,
        )
        if (updatedRows > 0) {
            return command.toState()
        }

        laptopProfileRepository.save(
            CrawledLaptopProfileEntityMapper.newProfile(
                laptop = entityManager.getReference(Laptop::class.java, command.laptopId),
                snapshot = profile,
            ),
        )
        return command.toState()
    }

    private fun UpsertCrawledLaptopProfileCommand.toState(): CrawledLaptopProfileState {
        return CrawledLaptopProfileState(
            laptopId = laptopId,
            profile = profile,
        )
    }
}
