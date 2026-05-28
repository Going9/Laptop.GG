package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
	scanBasePackages = [
		"going9.laptopgg.infrastructure.jpa.adapter.web",
		"going9.laptopgg.infrastructure.security",
		"going9.laptopgg.web",
	],
)
@EnableJpaRepositories(
	basePackages = [
		"going9.laptopgg.infrastructure.jpa.repository.shared",
		"going9.laptopgg.infrastructure.jpa.repository.web",
	],
)
class LaptopGgApplication

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
