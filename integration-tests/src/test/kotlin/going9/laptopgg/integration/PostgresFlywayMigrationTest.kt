package going9.laptopgg.integration

import going9.laptopgg.integration.support.PostgresIntegrationDatabase
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.DriverManager

@EnabledIfEnvironmentVariable(named = "POSTGRES_INTEGRATION_TESTS", matches = "true")
class PostgresFlywayMigrationTest {

    @Test
    fun `flyway migrations apply to an empty postgres database`() {
        val flyway = Flyway.configure()
            .dataSource(
                PostgresIntegrationDatabase.jdbcUrl,
                PostgresIntegrationDatabase.username,
                PostgresIntegrationDatabase.password,
            )
            .locations("classpath:db/migration")
            .baselineOnMigrate(false)
            .cleanDisabled(false)
            .load()

        flyway.clean()
        flyway.migrate()

        DriverManager.getConnection(
            PostgresIntegrationDatabase.jdbcUrl,
            PostgresIntegrationDatabase.username,
            PostgresIntegrationDatabase.password,
        ).use { connection ->
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
                "recommendation_score",
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
                        "detail_refresh_count",
                        "price_only_updated_count",
                        "failure_samples",
                        "started_at",
                        "ended_at",
                    )
                }
            }

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'recommendation_score'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val recommendationScoreColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(recommendationScoreColumns).contains(
                        "laptop_id",
                        "use_case",
                        "gate_score",
                        "static_score",
                        "budget_weight",
                        "updated_at",
                    )
                }
            }

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'comment'
                  and is_nullable = 'NO'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val requiredCommentColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(requiredCommentColumns).contains(
                        "id",
                        "author",
                        "content",
                        "pass_word",
                        "laptop_id",
                    )
                }
            }

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'comment_invalid_legacy'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val invalidCommentColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(invalidCommentColumns).contains(
                        "original_comment_id",
                        "reason",
                        "archived_at",
                    )
                }
            }
        }
    }
}
