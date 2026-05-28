package going9.laptopgg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "app.crawler.run-on-startup=false",
        "spring.datasource.url=jdbc:h2:mem:laptopgg-crawler-production-profile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@ActiveProfiles("postgres", "crawler")
class CrawlerJobProductionProfileTests {
    @Autowired
    lateinit var environment: Environment

    @Autowired
    lateinit var beanFactory: ListableBeanFactory

    @Test
    fun `crawler production profile does not run flyway migrations`() {
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean::class.java)).isFalse()
        assertThat(beanFactory.getBeanNamesForType(FlywayMigrationInitializer::class.java)).isEmpty()
    }
}
