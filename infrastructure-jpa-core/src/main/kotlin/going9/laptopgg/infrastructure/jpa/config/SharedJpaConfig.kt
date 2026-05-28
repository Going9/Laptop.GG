package going9.laptopgg.infrastructure.jpa.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = ["going9.laptopgg.persistence.model"])
class SharedJpaConfig
