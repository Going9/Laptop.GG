package going9.laptopgg.infrastructure.jpa.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(WebJpaRepositoryConfig::class)
@ComponentScan(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.adapter.web",
    ],
)
class WebJpaAdapterConfig
