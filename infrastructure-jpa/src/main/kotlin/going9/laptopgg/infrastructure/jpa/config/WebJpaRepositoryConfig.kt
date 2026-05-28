package going9.laptopgg.infrastructure.jpa.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration(proxyBeanMethods = false)
@Import(SharedJpaRepositoryConfig::class)
@EnableJpaRepositories(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.repository.web",
    ],
)
class WebJpaRepositoryConfig
