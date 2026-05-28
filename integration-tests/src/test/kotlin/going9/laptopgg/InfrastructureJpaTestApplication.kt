package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.CrawlerJpaRepositoryConfig
import going9.laptopgg.infrastructure.jpa.config.WebJpaRepositoryConfig
import going9.laptopgg.integration.config.IntegrationCrawlerUseCaseConfig
import going9.laptopgg.integration.config.IntegrationWebUseCaseConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.infrastructure.jpa.adapter.web",
    ],
)
@Import(
    WebJpaRepositoryConfig::class,
    CrawlerJpaRepositoryConfig::class,
    IntegrationWebUseCaseConfig::class,
    IntegrationCrawlerUseCaseConfig::class,
)
class InfrastructureJpaTestApplication
