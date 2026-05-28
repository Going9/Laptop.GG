package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.assembly.CrawlerProfileAssembler
import going9.laptopgg.application.crawler.profile.CpuClassifier
import going9.laptopgg.application.crawler.profile.CpuTokenResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.profile.GpuClassifier
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.ProfileScorePolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class CrawlerProfileUseCaseConfig {
    @Bean
    fun cpuTokenResolver(): CpuTokenResolver {
        return CrawlerProfileAssembler.createCpuTokenResolver()
    }

    @Bean
    fun cpuClassifier(cpuTokenResolver: CpuTokenResolver): CpuClassifier {
        return CrawlerProfileAssembler.createCpuClassifier(cpuTokenResolver)
    }

    @Bean
    fun gpuClassifier(): GpuClassifier {
        return CrawlerProfileAssembler.createGpuClassifier()
    }

    @Bean
    fun profileScorePolicy(): ProfileScorePolicy {
        return CrawlerProfileAssembler.createProfileScorePolicy()
    }

    @Bean
    fun crawledCpuManufacturerResolver(): CrawledCpuManufacturerResolver {
        return CrawlerProfileAssembler.createCrawledCpuManufacturerResolver()
    }

    @Bean
    fun crawledCpuModelResolver(cpuTokenResolver: CpuTokenResolver): CrawledCpuModelResolver {
        return CrawlerProfileAssembler.createCrawledCpuModelResolver(cpuTokenResolver)
    }

    @Bean
    fun crawledGraphicsModelResolver(gpuClassifier: GpuClassifier): CrawledGraphicsModelResolver {
        return CrawlerProfileAssembler.createCrawledGraphicsModelResolver(gpuClassifier)
    }

    @Bean
    fun laptopProfileFactory(
        cpuClassifier: CpuClassifier,
        gpuClassifier: GpuClassifier,
        profileScorePolicy: ProfileScorePolicy,
    ): LaptopProfileFactory {
        return CrawlerProfileAssembler.createLaptopProfileFactory(
            cpuClassifier = cpuClassifier,
            gpuClassifier = gpuClassifier,
            profileScorePolicy = profileScorePolicy,
        )
    }
}
