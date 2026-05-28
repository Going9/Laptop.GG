package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.CrawlerJpaRepositoryConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.job",
    ],
)
@Import(CrawlerJpaRepositoryConfig::class)
class CrawlerJobApplication

fun main(args: Array<String>) {
    runApplication<CrawlerJobApplication>(*args)
}
