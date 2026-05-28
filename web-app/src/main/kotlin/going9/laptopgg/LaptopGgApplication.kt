package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.WebJpaRepositoryConfig
import going9.laptopgg.infrastructure.security.PasswordHashProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
	scanBasePackages = [
		"going9.laptopgg.infrastructure.jpa.adapter.web",
		"going9.laptopgg.infrastructure.security",
		"going9.laptopgg.web",
	],
)
@EnableConfigurationProperties(PasswordHashProperties::class)
@Import(WebJpaRepositoryConfig::class)
class LaptopGgApplication

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
