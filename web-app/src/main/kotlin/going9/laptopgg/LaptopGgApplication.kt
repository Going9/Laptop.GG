package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
	scanBasePackages = [
		"going9.laptopgg.application.comment",
		"going9.laptopgg.application.laptop",
		"going9.laptopgg.application.recommendation",
		"going9.laptopgg.application.service",
		"going9.laptopgg.infrastructure.jpa",
		"going9.laptopgg.infrastructure.security",
		"going9.laptopgg.web",
	],
)
class LaptopGgApplication

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
