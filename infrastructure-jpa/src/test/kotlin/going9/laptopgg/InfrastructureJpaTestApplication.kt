package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.application.crawler",
        "going9.laptopgg.application.recommendation",
        "going9.laptopgg.application.service",
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.infrastructure.jpa.adapter.shared",
        "going9.laptopgg.infrastructure.jpa.adapter.web",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.repository.crawler",
        "going9.laptopgg.infrastructure.jpa.repository.shared",
        "going9.laptopgg.infrastructure.jpa.repository.web",
    ],
)
class InfrastructureJpaTestApplication
