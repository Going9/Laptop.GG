package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.WebJpaRepositoryConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
	scanBasePackages = [
		"going9.laptopgg.infrastructure.jpa.adapter.web",
		"going9.laptopgg.infrastructure.security",
		"going9.laptopgg.web",
	],
)
@Import(WebJpaRepositoryConfig::class)
class LaptopGgApplication

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
