package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import org.springframework.stereotype.Component

@Component
internal class CrawledLaptopPersistenceJpaAdapter(
    private val laptopRepository: CrawlerLaptopRepository,
) : CrawledLaptopPersistencePort {
    override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findWithUsageById(laptopId)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findAllWithUsageByProductCodeIn(listOf(productCode))
            .singleByCrawlerIdentity(identityName = "productCode", identityValue = productCode)
            ?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findAllWithUsageByDetailPageIn(listOf(detailPage))
            .singleByCrawlerIdentity(identityName = "detailPage", identityValue = detailPage)
            ?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
        if (productCodes.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findExistingByProductCodeIn(productCodes)
            .map { projection -> projection.toExistingCrawledLaptopSnapshot() }
    }

    override fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
        if (detailPages.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findExistingByDetailPageIn(detailPages)
            .map { projection -> projection.toExistingCrawledLaptopSnapshot() }
    }

    override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        return laptopRepository.save(CrawledLaptopEntityMapper.newLaptop(command)).toPersistedCrawledLaptopSnapshot()
    }

    override fun update(laptopId: Long, command: UpdateCrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        val laptop = laptopRepository.findWithUsageById(laptopId)
            ?: throw CrawlerResourceNotFoundException("Laptop", laptopId)
        CrawledLaptopEntityMapper.applyUpdate(laptop, command)
        return laptopRepository.save(laptop).toPersistedCrawledLaptopSnapshot()
    }

    private fun List<Laptop>.singleByCrawlerIdentity(identityName: String, identityValue: String): Laptop? {
        val uniqueLaptops = distinctBy { laptop -> laptop.id }
        if (uniqueLaptops.size > 1) {
            val ids = uniqueLaptops
                .map { laptop -> laptop.id?.toString() ?: "<unpersisted>" }
                .sorted()
                .joinToString(prefix = "[", postfix = "]")
            throw CrawlerInvalidStateException(
                "Multiple laptops found for $identityName=$identityValue; clean duplicate crawler identities before crawling: $ids",
            )
        }
        return uniqueLaptops.singleOrNull()
    }
}
