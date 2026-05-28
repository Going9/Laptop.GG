package going9.laptopgg

import going9.laptopgg.infrastructure.jpa.config.WebJpaAdapterConfig
import going9.laptopgg.infrastructure.security.PasswordHashAdapterConfig
import going9.laptopgg.web.config.WebConfigScanMarker
import going9.laptopgg.web.controller.WebControllerScanMarker
import going9.laptopgg.web.view.WebViewScanMarker
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(
	scanBasePackageClasses = [
		WebConfigScanMarker::class,
		WebControllerScanMarker::class,
		WebViewScanMarker::class,
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
