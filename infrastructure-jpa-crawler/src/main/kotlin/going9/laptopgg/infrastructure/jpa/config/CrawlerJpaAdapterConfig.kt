package going9.laptopgg.infrastructure.jpa.config

import going9.laptopgg.infrastructure.jpa.adapter.crawler.CrawlerJpaAdapterScanMarker
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(CrawlerJpaRepositoryConfig::class)
@ComponentScan(
    basePackageClasses = [
        CrawlerJpaAdapterScanMarker::class,
    ],
)
class CrawlerJpaAdapterConfig
