package going9.laptopgg.integration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.DriverManager
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@EnabledIfEnvironmentVariable(named = "POSTGRES_INTEGRATION_TESTS", matches = "true")
class PostgresFlywayMigrationTest {

    @Test
    fun `flyway migrations apply to an empty postgres database`() {
        val externalJdbcUrl = System.getenv("POSTGRES_INTEGRATION_JDBC_URL")
        val postgres = if (externalJdbcUrl == null) {
            PostgreSQLContainer(DockerImageName.parse("postgres:16"))
                .withDatabaseName("laptopgg_test")
                .withUsername("laptopgg")
                .withPassword("laptopgg")
                .also { it.start() }
        } else {
            null
        }
        val jdbcUrl = externalJdbcUrl ?: requireNotNull(postgres).jdbcUrl
        val username = System.getenv("POSTGRES_INTEGRATION_USERNAME") ?: postgres?.username ?: "laptopgg"
        val password = System.getenv("POSTGRES_INTEGRATION_PASSWORD") ?: postgres?.password ?: "laptopgg"

        try {
            val flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(false)
                .load()

            flyway.clean()
            flyway.migrate()

            DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
                val tableNames = connection.metaData.getTables(null, "public", "%", arrayOf("TABLE")).use { tables ->
                    generateSequence {
                        if (tables.next()) tables.getString("TABLE_NAME") else null
                    }.toSet()
                }

                assertThat(tableNames).contains(
                    "laptop",
                    "laptop_profile",
                    "laptop_usage",
                    "laptop_price_history",
                    "crawler_run",
                )

                connection.prepareStatement(
                    """
                    select column_name
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'crawler_run'
                    """.trimIndent(),
                ).use { statement ->
                    statement.executeQuery().use { resultSet ->
                        val crawlerRunColumns = generateSequence {
                            if (resultSet.next()) resultSet.getString("column_name") else null
                        }.toSet()

                        assertThat(crawlerRunColumns).contains(
                            "filter_profile",
                            "status",
                            "processed_count",
                            "failure_samples",
                            "started_at",
                            "ended_at",
                        )
                    }
                }
            }
        } finally {
            postgres?.stop()
        }
    }
}
