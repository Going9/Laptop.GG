package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.application.crawler",
        "going9.laptopgg.application.recommendation",
        "going9.laptopgg.application.service",
        "going9.laptopgg.infrastructure.jpa",
    ],
)
class InfrastructureJpaTestApplication
