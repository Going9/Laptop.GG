package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.profile.CrawledLaptopProfileState
import going9.laptopgg.application.crawler.profile.UpsertCrawledLaptopProfileCommand
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfilePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import org.springframework.stereotype.Component

@Component
class CrawledLaptopProfileJpaAdapter(
    private val laptopProfileRepository: CrawlerLaptopProfileRepository,
    private val laptopRepository: CrawlerLaptopRepository,
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
                    laptop = laptopRepository.getReferenceById(command.laptopId),
                    snapshot = command.profile,
                ),
            ),
        )
    }
}
