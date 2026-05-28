package going9.laptopgg.integration

import going9.laptopgg.integration.support.PostgresIntegrationDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "POSTGRES_INTEGRATION_TESTS", matches = "true")
class PostgresCrawlerIdentityPreflightSqlTest {
    private val preflightSql = Files.readString(resolveRepoPath("ops/sql/crawler-identity-preflight.sql"))

    private val flyway = Flyway.configure()
        .dataSource(
            PostgresIntegrationDatabase.jdbcUrl,
            PostgresIntegrationDatabase.username,
            PostgresIntegrationDatabase.password,
        )
        .locations("classpath:db/migration")
        .baselineOnMigrate(false)
        .cleanDisabled(false)
        .load()

    @BeforeEach
    fun resetDatabase() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `crawler identity preflight passes when crawler identities are unique`() {
        connection().use { connection ->
            connection.insertLaptop(id = 1L, productCode = "P1", detailPage = "https://example.com/p1")
            connection.insertLaptop(id = 2L, productCode = "P2", detailPage = "https://example.com/p2")

            assertThatCode {
                connection.executePreflight()
            }.doesNotThrowAnyException()
        }
    }

    @Test
    fun `crawler identity preflight fails when product code is duplicated`() {
        connection().use { connection ->
            connection.insertLaptop(id = 1L, productCode = "DUP", detailPage = "https://example.com/dup-a")
            connection.insertLaptop(id = 2L, productCode = "DUP", detailPage = "https://example.com/dup-b")

            assertThatThrownBy {
                connection.executePreflight()
            }.hasMessageContaining("duplicate product_code")
                .hasMessageContaining("crawler-identity-diagnostics.sql")
        }
    }

    @Test
    fun `crawler identity preflight fails when detail page is duplicated`() {
        connection().use { connection ->
            connection.insertLaptop(id = 1L, productCode = "P1", detailPage = "https://example.com/dup")
            connection.insertLaptop(id = 2L, productCode = "P2", detailPage = "https://example.com/dup")

            assertThatThrownBy {
                connection.executePreflight()
            }.hasMessageContaining("duplicate detail_page")
                .hasMessageContaining("crawler-identity-diagnostics.sql")
        }
    }

    private fun connection(): Connection {
        return DriverManager.getConnection(
            PostgresIntegrationDatabase.jdbcUrl,
            PostgresIntegrationDatabase.username,
            PostgresIntegrationDatabase.password,
        )
    }

    private fun Connection.executePreflight() {
        createStatement().use { statement ->
            statement.execute(preflightSql)
        }
    }

    private fun Connection.insertLaptop(id: Long, productCode: String, detailPage: String) {
        prepareStatement(
            """
            insert into public.laptop (id, name, image_url, detail_page, product_code)
            values (?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setLong(1, id)
            statement.setString(2, "Laptop $id")
            statement.setString(3, "https://example.com/laptop-$id.jpg")
            statement.setString(4, detailPage)
            statement.setString(5, productCode)
            statement.executeUpdate()
        }
    }

    private fun resolveRepoPath(path: String): Path {
        return listOf(Path.of(path), Path.of("..").resolve(path))
            .first(Files::exists)
    }
}
