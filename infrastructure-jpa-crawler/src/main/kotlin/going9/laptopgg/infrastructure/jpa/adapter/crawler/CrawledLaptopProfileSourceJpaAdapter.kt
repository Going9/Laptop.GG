package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.profile.port.CrawledLaptopProfileSourcePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CrawledLaptopProfileSourceJpaAdapter(
    private val laptopRepository: CrawlerLaptopRepository,
) : CrawledLaptopProfileSourcePort {
    override fun findAllWithUsageByIds(laptopIds: Collection<Long>): List<PersistedCrawledLaptopSnapshot> {
        if (laptopIds.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllWithUsageByIdIn(laptopIds)
            .map { laptop -> laptop.toPersistedCrawledLaptopSnapshot() }
    }

    override fun findIdsWithoutProfile(limit: Int): List<Long> {
        if (limit <= 0) {
            return emptyList()
        }
        return laptopRepository.findIdsWithoutProfile(PageRequest.of(0, limit))
    }
}
