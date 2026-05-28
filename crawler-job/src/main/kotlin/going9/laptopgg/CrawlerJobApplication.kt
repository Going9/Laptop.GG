package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.application.crawler",
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.infrastructure.jpa.adapter.shared",
        "going9.laptopgg.job",
        "going9.laptopgg.runner",
    ],
)
class CrawlerJobApplication

fun main(args: Array<String>) {
    runApplication<CrawlerJobApplication>(*args)
}
