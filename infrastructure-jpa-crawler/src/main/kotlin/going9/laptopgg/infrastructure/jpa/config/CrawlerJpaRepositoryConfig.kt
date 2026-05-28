package going9.laptopgg.infrastructure.jpa.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = ["going9.laptopgg.domain"])
@EnableJpaRepositories(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.repository.crawler",
        "going9.laptopgg.infrastructure.jpa.repository.shared",
    ],
)
class CrawlerJpaRepositoryConfig
