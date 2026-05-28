package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.config",
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.job",
        "going9.laptopgg.runner",
    ],
)
@EnableJpaRepositories(
    basePackages = [
        "going9.laptopgg.infrastructure.jpa.repository.crawler",
        "going9.laptopgg.infrastructure.jpa.repository.shared",
    ],
)
class CrawlerJobApplication

fun main(args: Array<String>) {
    runApplication<CrawlerJobApplication>(*args)
}
