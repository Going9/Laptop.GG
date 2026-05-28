package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.CrawlerJpaRepositoryConfig
import going9.laptopgg.job.config.CrawlerJobProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
    scanBasePackages = [
        "going9.laptopgg.infrastructure.jpa.adapter.crawler",
        "going9.laptopgg.job",
    ],
)
@EnableConfigurationProperties(CrawlerJobProperties::class)
@Import(CrawlerJpaRepositoryConfig::class)
class CrawlerJobApplication

fun main(args: Array<String>) {
    runApplication<CrawlerJobApplication>(*args)
}
