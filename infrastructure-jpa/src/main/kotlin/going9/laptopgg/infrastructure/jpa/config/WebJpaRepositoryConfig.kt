package going9.laptopgg.infrastructure.jpa.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration(proxyBeanMethods = false)
@Import(SharedJpaConfig::class)
@EntityScan(
    basePackages = [
        "going9.laptopgg.persistence.model.laptop",
        "going9.laptopgg.persistence.model.recommendation",
        "going9.laptopgg.persistence.model.web",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.repository.web",
    ],
)
class WebJpaRepositoryConfig
