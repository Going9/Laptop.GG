package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class CrawlerProfileResolverConfig {
    @Bean
    fun crawledCpuManufacturerResolver(): CrawledCpuManufacturerResolver {
        return CrawledCpuManufacturerResolver()
    }

    @Bean
    fun crawledCpuModelResolver(): CrawledCpuModelResolver {
        return CrawledCpuModelResolver()
    }

    @Bean
    fun crawledGraphicsModelResolver(): CrawledGraphicsModelResolver {
        return CrawledGraphicsModelResolver()
    }
}
