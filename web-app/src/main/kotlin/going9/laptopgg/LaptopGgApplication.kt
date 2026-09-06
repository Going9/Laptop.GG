package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.WebJpaAdapterConfig
import going9.laptopgg.infrastructure.security.PasswordHashAdapterConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
	scanBasePackages = [
		"going9.laptopgg.web.config",
		"going9.laptopgg.web.controller",
		"going9.laptopgg.web.view",
	],
)
@Import(
	WebJpaAdapterConfig::class,
	PasswordHashAdapterConfig::class,
)
class LaptopGgApplication

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
