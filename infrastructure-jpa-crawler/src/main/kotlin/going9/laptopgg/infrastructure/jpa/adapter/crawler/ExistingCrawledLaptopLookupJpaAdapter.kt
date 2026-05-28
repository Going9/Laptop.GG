package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.port.ExistingCrawledLaptopLookupPort
import going9.laptopgg.infrastructure.jpa.repository.crawler.ExistingCrawledLaptopLookupRepository
import org.springframework.stereotype.Component

@Component
internal class ExistingCrawledLaptopLookupJpaAdapter(
    private val laptopRepository: ExistingCrawledLaptopLookupRepository,
) : ExistingCrawledLaptopLookupPort {
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
}
