package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.CrawlerJpaAdapterConfig
import going9.laptopgg.job.config.CrawlerConfigScanMarker
import going9.laptopgg.job.config.CrawlerJobProperties
import going9.laptopgg.job.crawler.danawa.DanawaCrawlerScanMarker
import going9.laptopgg.job.crawler.orchestration.CrawlerOrchestrationScanMarker
import going9.laptopgg.job.runner.CrawlerRunnerScanMarker
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
    scanBasePackageClasses = [
        CrawlerConfigScanMarker::class,
        CrawlerRunnerScanMarker::class,
        CrawlerOrchestrationScanMarker::class,
        DanawaCrawlerScanMarker::class,
    ],
)
@EnableConfigurationProperties(CrawlerJobProperties::class)
@Import(CrawlerJpaAdapterConfig::class)
class CrawlerJobApplication

fun main(args: Array<String>) {
    runApplication<CrawlerJobApplication>(*args)
}
