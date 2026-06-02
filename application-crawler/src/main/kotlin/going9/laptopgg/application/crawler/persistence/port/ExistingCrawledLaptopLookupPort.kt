package going9.laptopgg.application.crawler.persistence.port

import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot

interface ExistingCrawledLaptopLookupPort {
    fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot>
    fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot>
}
