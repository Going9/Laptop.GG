package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.port.out.CrawledLaptopPort
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CrawledLaptopJpaAdapter(
    private val laptopRepository: CrawlerLaptopRepository,
) : CrawledLaptopPort {
    override fun findWithUsageById(laptopId: Long): Laptop? {
        return laptopRepository.findWithUsageById(laptopId)
    }

    override fun findAllWithUsageByIds(laptopIds: Collection<Long>): List<Laptop> {
        if (laptopIds.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllWithUsageByIdIn(laptopIds)
    }

    override fun findIdsWithoutProfile(limit: Int): List<Long> {
        if (limit <= 0) {
            return emptyList()
        }
        return laptopRepository.findIdsWithoutProfile(PageRequest.of(0, limit))
    }

    override fun findByProductCode(productCode: String): Laptop? {
        return laptopRepository.findByProductCode(productCode)
    }

    override fun findByDetailPage(detailPage: String): Laptop? {
        return laptopRepository.findByDetailPage(detailPage)
    }

    override fun findAllByProductCodes(productCodes: Collection<String>): List<Laptop> {
        if (productCodes.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllByProductCodeIn(productCodes)
    }

    override fun findAllByDetailPages(detailPages: Collection<String>): List<Laptop> {
        if (detailPages.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllByDetailPageIn(detailPages)
    }

    override fun save(laptop: Laptop): Laptop {
        return laptopRepository.save(laptop)
    }
}
