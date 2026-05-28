package going9.laptopgg.infrastructure.jpa.config

import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.persistence.model.crawler.CrawlerRun
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration(proxyBeanMethods = false)
@Import(SharedJpaConfig::class)
@EntityScan(
    basePackageClasses = [
        CrawlerRun::class,
        Laptop::class,
        RecommendationScore::class,
    ],
)
@EnableJpaRepositories(
    basePackageClasses = [
        CrawlerLaptopRepository::class,
    ],
)
class CrawlerJpaRepositoryConfig
