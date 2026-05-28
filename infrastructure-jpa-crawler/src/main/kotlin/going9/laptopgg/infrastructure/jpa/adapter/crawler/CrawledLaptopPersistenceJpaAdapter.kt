package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import org.springframework.stereotype.Component

@Component
class CrawledLaptopPersistenceJpaAdapter(
    private val laptopRepository: CrawlerLaptopRepository,
) : CrawledLaptopPersistencePort {
    override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findWithUsageById(laptopId)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findByProductCode(productCode)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findByDetailPage(detailPage)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findAllByProductCodes(productCodes: Collection<String>): List<PersistedCrawledLaptopSnapshot> {
        if (productCodes.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllByProductCodeIn(productCodes).map { laptop -> laptop.toPersistedCrawledLaptopSnapshot() }
    }

    override fun findAllByDetailPages(detailPages: Collection<String>): List<PersistedCrawledLaptopSnapshot> {
        if (detailPages.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findAllByDetailPageIn(detailPages).map { laptop -> laptop.toPersistedCrawledLaptopSnapshot() }
    }

    override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        return laptopRepository.save(CrawledLaptopEntityMapper.newLaptop(command)).toPersistedCrawledLaptopSnapshot()
    }

    override fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        val laptop = laptopRepository.findWithUsageById(laptopId)
            ?: throw IllegalArgumentException("Laptop not found: $laptopId")
        CrawledLaptopEntityMapper.applyUpdate(laptop, command)
        return laptopRepository.save(laptop).toPersistedCrawledLaptopSnapshot()
    }
}
