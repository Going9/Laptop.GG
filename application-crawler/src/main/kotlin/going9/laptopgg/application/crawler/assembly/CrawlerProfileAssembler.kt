package going9.laptopgg.application.crawler.assembly

import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory

object CrawlerProfileAssembler {
    fun createCrawledCpuManufacturerResolver(): CrawledCpuManufacturerResolver {
        return CrawledCpuManufacturerResolver()
    }

    fun createCrawledCpuModelResolver(): CrawledCpuModelResolver {
        return CrawledCpuModelResolver()
    }

    fun createCrawledGraphicsModelResolver(): CrawledGraphicsModelResolver {
        return CrawledGraphicsModelResolver()
    }

    internal fun createLaptopProfileFactory(): LaptopProfileFactory {
        return LaptopProfileFactory()
    }
}
