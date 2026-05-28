package going9.laptopgg.application.crawler.assembly

import going9.laptopgg.application.crawler.profile.CpuClassifier
import going9.laptopgg.application.crawler.profile.CpuTokenResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuManufacturerResolver
import going9.laptopgg.application.crawler.profile.CrawledCpuModelResolver
import going9.laptopgg.application.crawler.profile.CrawledGraphicsModelResolver
import going9.laptopgg.application.crawler.profile.GpuClassifier
import going9.laptopgg.application.crawler.profile.LaptopProfileFactory
import going9.laptopgg.application.crawler.profile.ProfileScorePolicy

object CrawlerProfileAssembler {
    fun createCpuTokenResolver(): CpuTokenResolver {
        return CpuTokenResolver()
    }

    fun createCpuClassifier(cpuTokenResolver: CpuTokenResolver): CpuClassifier {
        return CpuClassifier(cpuTokenResolver)
    }

    fun createGpuClassifier(): GpuClassifier {
        return GpuClassifier()
    }

    fun createProfileScorePolicy(): ProfileScorePolicy {
        return ProfileScorePolicy()
    }

    fun createCrawledCpuManufacturerResolver(): CrawledCpuManufacturerResolver {
        return CrawledCpuManufacturerResolver()
    }

    fun createCrawledCpuModelResolver(cpuTokenResolver: CpuTokenResolver): CrawledCpuModelResolver {
        return CrawledCpuModelResolver(cpuTokenResolver)
    }

    fun createCrawledGraphicsModelResolver(gpuClassifier: GpuClassifier): CrawledGraphicsModelResolver {
        return CrawledGraphicsModelResolver(gpuClassifier)
    }

    fun createLaptopProfileFactory(
        cpuClassifier: CpuClassifier,
        gpuClassifier: GpuClassifier,
        profileScorePolicy: ProfileScorePolicy,
    ): LaptopProfileFactory {
        return LaptopProfileFactory(
            cpuClassifier = cpuClassifier,
            gpuClassifier = gpuClassifier,
            profileScorePolicy = profileScorePolicy,
        )
    }
}
