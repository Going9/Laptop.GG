package going9.laptopgg.infrastructure.jpa.config

import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import going9.laptopgg.persistence.model.web.Comment
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration(proxyBeanMethods = false)
@Import(SharedJpaConfig::class)
@EntityScan(
    basePackageClasses = [
        Laptop::class,
        RecommendationScore::class,
        Comment::class,
    ],
)
@EnableJpaRepositories(
    basePackageClasses = [
        WebLaptopRepository::class,
    ],
)
class WebJpaRepositoryConfig
