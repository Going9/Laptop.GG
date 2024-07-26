package going9.laptopgg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.sql.DataSource

@SpringBootApplication
@EnableJpaRepositories
class LaptopGgApplication {

	@Bean
	fun init(dataSource: DataSource): CommandLineRunner {
		return CommandLineRunner {
			println("Database URL: ${dataSource.connection.metaData.url}")
			println("Database Username: ${dataSource.connection.metaData.userName}")
		}
	}
}

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
