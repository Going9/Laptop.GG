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
        val existingProfile = laptopProfileRepository.findByLaptopId(command.laptopId)
        if (existingProfile != null) {
            return if (CrawledLaptopProfileEntityMapper.applySnapshot(existingProfile, command.profile)) {
                CrawledLaptopProfileEntityMapper.toState(laptopProfileRepository.save(existingProfile))
            } else {
                CrawledLaptopProfileEntityMapper.toState(existingProfile)
            }
        }

        return CrawledLaptopProfileEntityMapper.toState(
            laptopProfileRepository.save(
                CrawledLaptopProfileEntityMapper.newProfile(
                    laptop = entityManager.getReference(Laptop::class.java, command.laptopId),
                    snapshot = command.profile,
                ),
            ),
        )
    }
}
