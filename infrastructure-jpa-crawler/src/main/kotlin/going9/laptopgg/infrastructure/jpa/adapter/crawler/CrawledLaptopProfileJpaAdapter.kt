package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.port.out.CrawledLaptopProfilePort
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.infrastructure.jpa.repository.shared.LaptopProfileRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CrawledLaptopProfileJpaAdapter(
    private val laptopProfileRepository: LaptopProfileRepository,
) : CrawledLaptopProfilePort {
    override fun findByLaptopId(laptopId: Long): LaptopProfile? {
        return laptopProfileRepository.findByLaptopId(laptopId)
    }

    override fun save(laptopProfile: LaptopProfile): LaptopProfile {
        return laptopProfileRepository.save(laptopProfile)
    }

    override fun findLaptopIdsWithIncompleteStaticScores(limit: Int): List<Long> {
        if (limit <= 0) {
            return emptyList()
        }
        return laptopProfileRepository.findLaptopIdsWithIncompleteStaticScores(PageRequest.of(0, limit))
    }
}
