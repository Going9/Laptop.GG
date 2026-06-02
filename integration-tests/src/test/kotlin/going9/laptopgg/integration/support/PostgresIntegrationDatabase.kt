package going9.laptopgg.integration.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

internal object PostgresIntegrationDatabase {
    private val externalJdbcUrl = System.getenv("POSTGRES_INTEGRATION_JDBC_URL")
        ?.takeIf { it.isNotBlank() }

    private val postgresContainer: PostgreSQLContainer<*>? by lazy {
        if (System.getenv("POSTGRES_INTEGRATION_TESTS") == "true" && externalJdbcUrl == null) {
            PostgreSQLContainer(DockerImageName.parse("postgres:16"))
                .withDatabaseName(DEFAULT_DATABASE)
                .withUsername(DEFAULT_USERNAME)
                .withPassword(DEFAULT_PASSWORD)
                .also { it.start() }
        } else {
            null
        }
    }

    val jdbcUrl: String
        get() = externalJdbcUrl ?: requireNotNull(postgresContainer).jdbcUrl

    val username: String
        get() = System.getenv("POSTGRES_INTEGRATION_USERNAME")
            ?: postgresContainer?.username
            ?: DEFAULT_USERNAME

    val password: String
        get() = System.getenv("POSTGRES_INTEGRATION_PASSWORD")
            ?: postgresContainer?.password
            ?: DEFAULT_PASSWORD

    fun registerSpringDatasource(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { jdbcUrl }
        registry.add("spring.datasource.username") { username }
        registry.add("spring.datasource.password") { password }
    }

    private const val DEFAULT_DATABASE = "laptopgg_test"
    private const val DEFAULT_USERNAME = "laptopgg"
    private const val DEFAULT_PASSWORD = "laptopgg"
}
