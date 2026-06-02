package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.CrawlerJpaAdapterConfig
import going9.laptopgg.infrastructure.jpa.config.WebJpaAdapterConfig
import going9.laptopgg.integration.config.IntegrationCrawlerUseCaseConfig
import going9.laptopgg.integration.config.IntegrationWebUseCaseConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
    scanBasePackageClasses = [
        IntegrationWebUseCaseConfig::class,
        IntegrationCrawlerUseCaseConfig::class,
    ],
)
@Import(
    WebJpaAdapterConfig::class,
    CrawlerJpaAdapterConfig::class,
)
class InfrastructureJpaTestApplication
