package going9.laptopgg.infrastructure.jpa.config

import going9.laptopgg.infrastructure.jpa.adapter.web.WebJpaAdapterScanMarker
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(WebJpaRepositoryConfig::class)
@ComponentScan(
    basePackageClasses = [
        WebJpaAdapterScanMarker::class,
    ],
)
class WebJpaAdapterConfig
